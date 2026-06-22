package com.rideflow.matching.application.usecase;

import com.rideflow.matching.application.port.out.DispatchEventPublisher;
import com.rideflow.matching.application.port.out.ProcessedEventStore;
import com.rideflow.matching.application.port.out.RideRepository;
import com.rideflow.matching.domain.event.RideAssigned;
import com.rideflow.matching.domain.event.RideDispatchFailed;
import com.rideflow.matching.domain.model.DispatchAttempt;
import com.rideflow.matching.domain.model.DispatchCandidate;
import com.rideflow.matching.domain.model.Ride;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * The atomic write boundary of dispatch. Each public method is a single
 * transaction bundling: idempotency record + ride outcome + attempt audit +
 * outbox event. Either all four land, or none do.
 *
 * <p><b>Why a separate bean from {@link DispatchRideUseCase}?</b> The dispatch
 * lock must be <em>held across the commit</em> and released only afterwards.
 * That puts the transaction boundary <em>inside</em> the lock scope, not the
 * other way round. The orchestrator holds the lock and calls into this bean, so
 * Spring's {@code @Transactional} proxy actually applies (a self-invocation
 * would silently bypass it).
 */
@Service
public class DispatchCommitService {

    private static final Logger log = LoggerFactory.getLogger(DispatchCommitService.class);

    private final RideRepository        rideRepository;
    private final DispatchEventPublisher eventPublisher;
    private final ProcessedEventStore   processedEvents;

    public DispatchCommitService(RideRepository rideRepository,
                                 DispatchEventPublisher eventPublisher,
                                 ProcessedEventStore processedEvents) {
        this.rideRepository  = rideRepository;
        this.eventPublisher  = eventPublisher;
        this.processedEvents = processedEvents;
    }

    /**
     * Commit a successful match for {@code winner}.
     *
     * @return {@code true} if the assignment committed; {@code false} if the DB
     *         rejected it (driver already {@code ASSIGNED} elsewhere, or this
     *         ride already dispatched) — the orchestrator then tries the next
     *         candidate. A {@code false} here means the surrounding lock gave a
     *         false sense of exclusivity (e.g. Redis lost the lock) and the
     *         partial-unique index did its job as the real arbiter.
     */
    @Transactional
    public boolean commitAssignment(java.util.UUID inboundEventId,
                                    String consumerGroup,
                                    Ride ride,
                                    DispatchCandidate winner,
                                    List<DispatchAttempt> attempts) {
        Instant now = Instant.now();
        Ride assigned = ride.assignTo(winner.driverId(), winner.score(), now);

        try {
            processedEvents.markProcessed(inboundEventId, consumerGroup);
            rideRepository.saveOutcome(assigned, attempts);

            eventPublisher.publishRideAssigned(new RideAssigned(
                    assigned.id(),
                    assigned.riderId(),
                    winner.driverId(),
                    assigned.pickup().lat(),
                    assigned.pickup().lng(),
                    assigned.dropoff().lat(),
                    assigned.dropoff().lng(),
                    assigned.vehicleType().name(),
                    winner.score(),
                    winner.distanceMeters(),
                    attempts.size(),
                    now));

            log.info("Ride {} assigned to driver {} score={} attempts={}",
                    assigned.id(), winner.driverId(),
                    String.format("%.3f", winner.score()), attempts.size());
            return true;

        } catch (DataIntegrityViolationException e) {
            // Partial-unique on assigned_driver_id (or ride PK) fired. Another
            // dispatch beat us to this driver despite the lock. Roll back and
            // let the orchestrator fall through to the next candidate.
            log.warn("Assignment lost race for ride {} driver {} — {}",
                    ride.id(), winner.driverId(), e.getMostSpecificCause().getMessage());
            throw e;   // rollback; orchestrator catches and continues
        }
    }

    /** Commit a dispatch failure after the radius ladder is exhausted. */
    @Transactional
    public void commitFailure(java.util.UUID inboundEventId,
                              String consumerGroup,
                              Ride ride,
                              List<DispatchAttempt> attempts,
                              String reason,
                              int lastRadiusMeters) {
        Instant now = Instant.now();
        Ride failed = ride.fail(reason, now);

        processedEvents.markProcessed(inboundEventId, consumerGroup);
        rideRepository.saveOutcome(failed, attempts);

        eventPublisher.publishRideDispatchFailed(new RideDispatchFailed(
                failed.id(),
                failed.riderId(),
                failed.vehicleType().name(),
                reason,
                attempts.size(),
                lastRadiusMeters,
                now));

        log.info("Ride {} dispatch failed: {} after {} attempt(s), last radius {}m",
                failed.id(), reason, attempts.size(), lastRadiusMeters);
    }

    /**
     * Re-dispatch counterpart of {@link #commitAssignment}: the ride already has
     * a persisted row (it was {@code ASSIGNED} before the offer was rejected), so
     * we {@code updateOutcome} rather than insert, and {@link Ride#reassignTo}
     * bumps the redispatch counter. Idempotency is keyed on the inbound
     * {@code ride.rejected} event id.
     */
    @Transactional
    public boolean commitReassignment(java.util.UUID inboundEventId,
                                      String consumerGroup,
                                      Ride ride,
                                      DispatchCandidate winner,
                                      List<DispatchAttempt> attempts) {
        Instant now = Instant.now();
        Ride reassigned = ride.reassignTo(winner.driverId(), winner.score(), now);

        try {
            processedEvents.markProcessed(inboundEventId, consumerGroup);
            rideRepository.updateOutcome(reassigned, attempts);

            eventPublisher.publishRideAssigned(new RideAssigned(
                    reassigned.id(),
                    reassigned.riderId(),
                    winner.driverId(),
                    reassigned.pickup().lat(),
                    reassigned.pickup().lng(),
                    reassigned.dropoff().lat(),
                    reassigned.dropoff().lng(),
                    reassigned.vehicleType().name(),
                    winner.score(),
                    winner.distanceMeters(),
                    attempts.size(),
                    now));

            log.info("Ride {} re-assigned to driver {} score={} (redispatch #{})",
                    reassigned.id(), winner.driverId(),
                    String.format("%.3f", winner.score()), reassigned.redispatchCount());
            return true;

        } catch (DataIntegrityViolationException e) {
            log.warn("Re-assignment lost race for ride {} driver {} — {}",
                    ride.id(), winner.driverId(), e.getMostSpecificCause().getMessage());
            throw e;   // rollback; orchestrator catches and continues
        }
    }

    /**
     * Commit a re-dispatch failure (ladder exhausted on a re-dispatch, or the
     * redispatch limit was hit). Updates the existing row rather than inserting.
     */
    @Transactional
    public void commitRedispatchFailure(java.util.UUID inboundEventId,
                                        String consumerGroup,
                                        Ride ride,
                                        String reason) {
        Instant now = Instant.now();
        Ride failed = ride.fail(reason, now);

        processedEvents.markProcessed(inboundEventId, consumerGroup);
        rideRepository.updateOutcome(failed, List.of());

        eventPublisher.publishRideDispatchFailed(new RideDispatchFailed(
                failed.id(),
                failed.riderId(),
                failed.vehicleType().name(),
                reason,
                0,
                0,
                now));

        log.info("Ride {} re-dispatch failed: {}", failed.id(), reason);
    }
}
