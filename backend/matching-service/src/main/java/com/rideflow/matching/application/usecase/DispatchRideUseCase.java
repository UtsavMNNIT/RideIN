package com.rideflow.matching.application.usecase;

import com.rideflow.matching.application.port.out.DriverLock;
import com.rideflow.matching.application.port.out.DriverLockService;
import com.rideflow.matching.application.port.out.ProcessedEventStore;
import com.rideflow.matching.application.port.out.RideRepository;
import com.rideflow.matching.domain.model.DispatchAttempt;
import com.rideflow.matching.domain.model.DispatchCandidate;
import com.rideflow.matching.domain.model.GeoPoint;
import com.rideflow.matching.domain.model.Ride;
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

        List<DispatchAttempt> attempts = new ArrayList<>();
        int attemptNo  = 0;
        int lastRadius = props.attempts().get(0);

        // ---- 2. Radius-expansion ladder ------------------------------------
        for (int radius : props.attempts()) {
            attemptNo++;
            lastRadius = radius;
            long attemptStart = System.nanoTime();

            List<DispatchCandidate> raw = candidateProvider.findCandidates(
                    ride.pickup(), ride.vehicleType(), radius, props.candidateLimit());

            if (raw.isEmpty()) {
                attempts.add(DispatchAttempt.noCandidates(attemptNo, radius, elapsedMs(attemptStart)));
                log.debug("Ride {} attempt {} radius {}m: no candidates", ride.id(), attemptNo, radius);
                continue;
            }

            List<DispatchCandidate> ranked = scorer.rank(raw, ride.pickup(), radius);

            // ---- 3/4/5. Lock best-first ------------------------------------
            DispatchCandidate matched = tryLockAndCommit(cmd, ride, ranked, attempts,
                    attemptNo, radius, raw.size(), attemptStart);
            if (matched != null) {
                count("dispatch.outcome", "result", "matched");
                return;
            }
            // Every candidate this ring was contended or lost the DB race — widen.
            log.debug("Ride {} attempt {} radius {}m: {} candidates, none lockable; expanding",
                    ride.id(), attemptNo, radius, raw.size());
        }

        // ---- 6. Ladder exhausted -------------------------------------------
        String reason = attempts.stream().allMatch(a -> a.candidatesFound() == 0)
                ? "NO_DRIVERS_IN_RANGE"
                : "ALL_CANDIDATES_CONTENDED";
        commitService.commitFailure(cmd.eventId(), consumerGroup, ride, attempts, reason, lastRadius);
        count("dispatch.outcome", "result", "failed");
    }

    /**
     * Walk the ranked candidates, attempting lock-then-commit on each. Returns
     * the matched candidate, or {@code null} if every candidate this ring was
     * either contended (lock held elsewhere) or lost the DB uniqueness race.
     */
    private DispatchCandidate tryLockAndCommit(DispatchCommand cmd, Ride ride,
                                               List<DispatchCandidate> ranked,
                                               List<DispatchAttempt> attempts,
                                               int attemptNo, int radius, int found,
                                               long attemptStart) {
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
                    boolean committed = commitService.commitAssignment(
                            cmd.eventId(), consumerGroup, ride, c, finalAttempts);
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
