package com.rideflow.matching.application.usecase;

import com.rideflow.matching.application.port.out.DriverLock;
import com.rideflow.matching.application.port.out.DriverLockService;
import com.rideflow.matching.application.port.out.ProcessedEventStore;
import com.rideflow.matching.application.port.out.RideRepository;
import com.rideflow.matching.domain.model.DispatchAttempt;
import com.rideflow.matching.domain.model.DispatchCandidate;
import com.rideflow.matching.domain.model.GeoPoint;
import com.rideflow.matching.domain.model.Ride;
import com.rideflow.matching.domain.model.RideStatus;
import com.rideflow.matching.domain.model.VehicleType;
import com.rideflow.matching.domain.repository.DispatchCandidateProvider;
import com.rideflow.matching.domain.service.DispatchScorer;
import com.rideflow.matching.infrastructure.config.DispatchProperties;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates dispatch for one {@code RideRequested}.
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li><b>Dedupe.</b> Skip if this envelope id was already processed or this
 *       ride already has a dispatch outcome (at-least-once delivery).</li>
 *   <li><b>Radius-expansion ladder.</b> Search progressively wider rings
 *       (3km → 5km → 8km). Stop at the first ring that yields a match — closer
 *       rings give better ETAs, so we never widen unnecessarily.</li>
 *   <li><b>Query.</b> Pull the K nearest dispatchable drivers from Redis-Geo
 *       (already filtered to ONLINE + fresh + right vehicle class).</li>
 *   <li><b>Score &amp; rank.</b> Blend distance + heading into a single score,
 *       best-first.</li>
 *   <li><b>Lock best-first.</b> Try the distributed lock on each candidate in
 *       score order; the first one we win is the match. Contention ⇒ skip to
 *       the next candidate (never queue — that inflates p99).</li>
 *   <li><b>Commit under lock.</b> Persist the assignment + outbox event while
 *       holding the lock; release only after commit. The DB partial-unique
 *       index is the final arbiter against double-booking.</li>
 * </ol>
 *
 * <h2>Transaction / lock ordering</h2>
 * The lock is acquired here, the commit runs in {@link DispatchCommitService}
 * (a distinct bean, so {@code @Transactional} applies), and the lock is released
 * in a {@code finally} <em>after</em> the commit returns — closing the window in
 * which a freed driver could be grabbed before our row is durable.
 *
 * <h2>Failure semantics</h2>
 * A hard lock-infrastructure failure ({@link
 * com.rideflow.matching.application.port.out.LockAcquisitionException}) is
 * allowed to propagate: the consumer does not ack, Kafka redelivers, and we
 * retry rather than ever assign without confirmed exclusion (fail closed).
 */
@Service
public class DispatchRideUseCase {

    private static final Logger log = LoggerFactory.getLogger(DispatchRideUseCase.class);

    private final DispatchCandidateProvider candidateProvider;
    private final DispatchScorer            scorer;
    private final DriverLockService         lockService;
    private final DispatchCommitService     commitService;
    private final ProcessedEventStore       processedEvents;
    private final RideRepository            rideRepository;
    private final DispatchProperties        props;
    private final String                    consumerGroup;

    private final MeterRegistry registry;
    private final Timer         dispatchTimer;

    public DispatchRideUseCase(DispatchCandidateProvider candidateProvider,
                               DispatchScorer scorer,
                               DriverLockService lockService,
                               DispatchCommitService commitService,
                               ProcessedEventStore processedEvents,
                               RideRepository rideRepository,
                               DispatchProperties props,
                               MeterRegistry registry,
                               @Value("${spring.kafka.consumer.group-id}") String consumerGroup) {
        this.candidateProvider = candidateProvider;
        this.scorer            = scorer;
        this.lockService       = lockService;
        this.commitService     = commitService;
        this.processedEvents   = processedEvents;
        this.rideRepository    = rideRepository;
        this.props             = props;
        this.consumerGroup     = consumerGroup;
        this.registry          = registry;
        this.dispatchTimer     = registry.timer("dispatch.duration");
    }

    public void dispatch(DispatchCommand cmd) {
        long startNanos = System.nanoTime();
        try {
            doDispatch(cmd);
        } finally {
            dispatchTimer.record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        }
    }

    private void doDispatch(DispatchCommand cmd) {
        // ---- 1. Idempotency -------------------------------------------------
        if (processedEvents.isProcessed(cmd.eventId(), consumerGroup)
                || rideRepository.existsById(cmd.rideId())) {
            log.debug("Skipping already-dispatched ride {} (eventId={})", cmd.rideId(), cmd.eventId());
            count("dispatch.outcome", "result", "duplicate");
            return;
        }

        Ride ride = Ride.requested(
                cmd.rideId(), cmd.riderId(), cmd.pickup(), cmd.dropoff(),
                cmd.vehicleType(), cmd.requestedAt()).beginDispatch();

        runLadder(cmd.eventId(), ride, Set.of(), false);
    }

    /**
     * Re-dispatch a ride after its assigned driver rejected the offer or it
     * expired (driven by {@code ride.rejected}). Idempotent on the rejection
     * event id; a stale rejection (the ride is no longer ASSIGNED to that
     * driver) is acknowledged and skipped. Bounded by
     * {@code rideflow.dispatch.max-redispatches}.
     */
    public void redispatch(UUID eventId, UUID rideId, UUID rejectedDriverId) {
        if (processedEvents.isProcessed(eventId, consumerGroup)) {
            log.debug("Skipping already-processed rejection eventId={}", eventId);
            count("redispatch.outcome", "result", "duplicate");
            return;
        }

        Ride existing = rideRepository.findById(rideId).orElse(null);

        // Only re-dispatch a ride still ASSIGNED to the driver who rejected; a
        // missing ride or any other state is a stale/duplicate rejection — ack
        // it so the offset advances, but do nothing else.
        if (existing == null
                || existing.status() != RideStatus.ASSIGNED
                || !rejectedDriverId.equals(existing.assignedDriverId())) {
            log.debug("Ride {} not re-dispatchable for rejecting driver {} (ride={}); acking & skipping",
                    rideId, rejectedDriverId, existing);
            processedEvents.markProcessed(eventId, consumerGroup);
            count("redispatch.outcome", "result", "skipped");
            return;
        }

        if (existing.redispatchCount() >= props.maxRedispatches()) {
            log.info("Ride {} hit re-dispatch limit {}; failing", rideId, props.maxRedispatches());
            commitService.commitRedispatchFailure(eventId, consumerGroup, existing, "MAX_REDISPATCH_EXCEEDED");
            count("redispatch.outcome", "result", "exhausted");
            return;
        }

        // Exclude the driver who just rejected so we never re-offer the same one.
        runLadder(eventId, existing.beginRedispatch(), Set.of(rejectedDriverId), true);
    }

    /**
     * Shared radius-expansion ladder for both initial dispatch and re-dispatch.
     * {@code dispatching} must already be in {@link RideStatus#DISPATCHING}.
     * When {@code redispatch} is true the commit updates the existing ride row
     * (bumping the redispatch counter) instead of inserting a new one.
     */
    private void runLadder(UUID eventId, Ride dispatching, Set<UUID> excluded, boolean redispatch) {
        String outcome = redispatch ? "redispatch.outcome" : "dispatch.outcome";

        List<DispatchAttempt> attempts = new ArrayList<>();
        int attemptNo  = 0;
        int lastRadius = props.attempts().get(0);

        // ---- Radius-expansion ladder ---------------------------------------
        for (int radius : props.attempts()) {
            attemptNo++;
            lastRadius = radius;
            long attemptStart = System.nanoTime();

            List<DispatchCandidate> raw = candidateProvider.findCandidates(
                    dispatching.pickup(), dispatching.vehicleType(), radius, props.candidateLimit(), excluded);

            if (raw.isEmpty()) {
                attempts.add(DispatchAttempt.noCandidates(attemptNo, radius, elapsedMs(attemptStart)));
                log.debug("Ride {} attempt {} radius {}m: no candidates", dispatching.id(), attemptNo, radius);
                continue;
            }

            List<DispatchCandidate> ranked = scorer.rank(raw, dispatching.pickup(), radius);

            // ---- Lock best-first -------------------------------------------
            DispatchCandidate matched = tryLockAndCommit(eventId, dispatching, ranked, attempts,
                    attemptNo, radius, raw.size(), attemptStart, redispatch);
            if (matched != null) {
                count(outcome, "result", "matched");
                return;
            }
            // Every candidate this ring was contended or lost the DB race — widen.
            log.debug("Ride {} attempt {} radius {}m: {} candidates, none lockable; expanding",
                    dispatching.id(), attemptNo, radius, raw.size());
        }

        // ---- Ladder exhausted ----------------------------------------------
        String reason = attempts.stream().allMatch(a -> a.candidatesFound() == 0)
                ? "NO_DRIVERS_IN_RANGE"
                : "ALL_CANDIDATES_CONTENDED";
        if (redispatch) {
            commitService.commitRedispatchFailure(eventId, consumerGroup, dispatching, reason);
        } else {
            commitService.commitFailure(eventId, consumerGroup, dispatching, attempts, reason, lastRadius);
        }
        count(outcome, "result", "failed");
    }

    /**
     * Walk the ranked candidates, attempting lock-then-commit on each. Returns
     * the matched candidate, or {@code null} if every candidate this ring was
     * either contended (lock held elsewhere) or lost the DB uniqueness race.
     */
    private DispatchCandidate tryLockAndCommit(UUID eventId, Ride ride,
                                               List<DispatchCandidate> ranked,
                                               List<DispatchAttempt> attempts,
                                               int attemptNo, int radius, int found,
                                               long attemptStart, boolean redispatch) {
        for (DispatchCandidate c : ranked) {
            try (DriverLock lock = lockService.tryAcquire(c.driverId())) {
                if (!lock.isAcquired()) {
                    count("dispatch.candidate", "result", "contended");
                    continue;                       // locked elsewhere → next best
                }

                DispatchAttempt winning = DispatchAttempt.success(
                        attemptNo, radius, found, c, elapsedMs(attemptStart));
                List<DispatchAttempt> finalAttempts = append(attempts, winning);

                try {
                    boolean committed = redispatch
                            ? commitService.commitReassignment(eventId, consumerGroup, ride, c, finalAttempts)
                            : commitService.commitAssignment(eventId, consumerGroup, ride, c, finalAttempts);
                    if (committed) {
                        return c;
                    }
                } catch (DataIntegrityViolationException e) {
                    // Lost the DB race under a (stale) lock — try the next driver.
                    count("dispatch.candidate", "result", "lost_race");
                }
                // released by try-with-resources, then loop to next candidate
            }
        }
        return null;
    }

    private int elapsedMs(long startNanos) {
        return (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private void count(String name, String tagKey, String tagVal) {
        registry.counter(name, tagKey, tagVal).increment();
    }

    private static List<DispatchAttempt> append(List<DispatchAttempt> base, DispatchAttempt extra) {
        List<DispatchAttempt> out = new ArrayList<>(base.size() + 1);
        out.addAll(base);
        out.add(extra);
        return out;
    }

    /**
     * Decoupled input for one dispatch — built by the Kafka consumer from the
     * inbound envelope. Carries the envelope id (for idempotency) alongside the
     * ride fields.
     */
    public record DispatchCommand(
            UUID        eventId,
            UUID        rideId,
            UUID        riderId,
            GeoPoint    pickup,
            GeoPoint    dropoff,
            VehicleType vehicleType,
            Instant     requestedAt) {}
}
