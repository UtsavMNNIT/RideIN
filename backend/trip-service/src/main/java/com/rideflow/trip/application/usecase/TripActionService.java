package com.rideflow.trip.application.usecase;

import com.rideflow.trip.application.port.out.TripEventPublisher;
import com.rideflow.trip.application.port.out.TripRepository;
import com.rideflow.trip.domain.event.RideAccepted;
import com.rideflow.trip.domain.event.RideCancelled;
import com.rideflow.trip.domain.event.RideCompleted;
import com.rideflow.trip.domain.event.RideRejected;
import com.rideflow.trip.domain.event.RideStarted;
import com.rideflow.trip.domain.exception.TripActionForbiddenException;
import com.rideflow.trip.domain.exception.TripNotFoundException;
import com.rideflow.trip.domain.model.CancelledBy;
import com.rideflow.trip.domain.model.Trip;
import com.rideflow.trip.domain.model.TripMetrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * The atomic write boundary for every trip transition. Each method is one
 * transaction: load (with {@code @Version} optimistic locking) → domain
 * transition → save → enqueue the outbox event. A concurrent conflicting write
 * (e.g. accept racing the expiry sweeper) loses on the version check and the
 * caller gets a 409.
 *
 * <p>Driver actions are authorized against {@code trip.driverId}; cancel is
 * authorized against whichever party (rider or driver) the caller claims to be.
 * Identity always comes from the gateway-validated {@code X-User-Id} — never the
 * path alone.
 */
@Service
public class TripActionService {

    private static final Logger log = LoggerFactory.getLogger(TripActionService.class);

    private final TripRepository     trips;
    private final TripEventPublisher events;

    public TripActionService(TripRepository trips, TripEventPublisher events) {
        this.trips  = trips;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Trip get(UUID rideId, UUID callerId) {
        Trip trip = load(rideId);
        if (!callerId.equals(trip.riderId()) && !callerId.equals(trip.driverId())) {
            throw new TripActionForbiddenException("caller is not a party to this trip");
        }
        return trip;
    }

    @Transactional
    public Trip accept(UUID rideId, UUID driverId) {
        Trip trip = requireDriver(rideId, driverId);
        Instant now = Instant.now();
        Trip next = trips.save(trip.accept(now));
        events.publishRideAccepted(new RideAccepted(next.id(), next.riderId(), next.driverId(), now));
        log.info("Trip {} ACCEPTED by driver {}", rideId, driverId);
        return next;
    }

    @Transactional
    public Trip reject(UUID rideId, UUID driverId) {
        Trip trip = requireDriver(rideId, driverId);
        Instant now = Instant.now();
        Trip next = trips.save(trip.reject(now));
        events.publishRideRejected(new RideRejected(
                next.id(), next.riderId(), next.driverId(), next.rejectReason(), now));
        log.info("Trip {} REJECTED by driver {}", rideId, driverId);
        return next;
    }

    @Transactional
    public Trip arrive(UUID rideId, UUID driverId) {
        Trip trip = requireDriver(rideId, driverId);
        Trip next = trips.save(trip.arrive(Instant.now()));
        log.info("Trip {} ARRIVED (driver {})", rideId, driverId);
        return next;   // internal-only transition, no event
    }

    @Transactional
    public Trip start(UUID rideId, UUID driverId) {
        Trip trip = requireDriver(rideId, driverId);
        Instant now = Instant.now();
        Trip next = trips.save(trip.start(now));
        events.publishRideStarted(RideStarted.of(next.id(), next.riderId(), next.driverId(), now));
        log.info("Trip {} STARTED (driver {})", rideId, driverId);
        return next;
    }

    @Transactional
    public Trip complete(UUID rideId, UUID driverId, TripMetrics metrics) {
        Trip trip = requireDriver(rideId, driverId);
        Instant now = Instant.now();
        Trip next = trips.save(trip.complete(metrics, now));
        events.publishRideCompleted(new RideCompleted(
                next.id(), next.riderId(), next.driverId(), now,
                metrics.finalDistanceMeters(), metrics.finalDurationSeconds(), null, null));
        log.info("Trip {} COMPLETED (driver {}, {}m/{}s)",
                rideId, driverId, metrics.finalDistanceMeters(), metrics.finalDurationSeconds());
        return next;
    }

    /**
     * Cancel by the rider or driver. {@code callerId}/{@code role} come from the
     * gateway; the caller must be the trip's rider or driver.
     */
    @Transactional
    public Trip cancel(UUID rideId, UUID callerId, String role, String reason) {
        Trip trip = load(rideId);
        CancelledBy by = resolveCanceller(trip, callerId, role);
        Instant now = Instant.now();
        Trip next = trips.save(trip.cancel(by, reason, now));
        events.publishRideCancelled(new RideCancelled(
                next.id(), next.riderId(), next.driverId(), now, by.name(), reason));
        log.info("Trip {} CANCELLED by {} ({})", rideId, by, reason);
        return next;
    }

    /** System action invoked by the expiry sweeper. No actor authorization. */
    @Transactional
    public void expire(UUID rideId) {
        Trip trip = load(rideId);
        Instant now = Instant.now();
        Trip next = trips.save(trip.expire(now));
        events.publishRideRejected(new RideRejected(
                next.id(), next.riderId(), next.driverId(), next.rejectReason(), now));
        log.info("Trip {} offer EXPIRED", rideId);
    }

    private Trip requireDriver(UUID rideId, UUID driverId) {
        Trip trip = load(rideId);
        if (!driverId.equals(trip.driverId())) {
            throw new TripActionForbiddenException("driver " + driverId + " is not assigned to this trip");
        }
        return trip;
    }

    private CancelledBy resolveCanceller(Trip trip, UUID callerId, String role) {
        if ("RIDER".equalsIgnoreCase(role) && callerId.equals(trip.riderId()))  return CancelledBy.RIDER;
        if ("DRIVER".equalsIgnoreCase(role) && callerId.equals(trip.driverId())) return CancelledBy.DRIVER;
        throw new TripActionForbiddenException("caller is not a party to this trip");
    }

    private Trip load(UUID rideId) {
        return trips.findById(rideId).orElseThrow(() -> new TripNotFoundException(rideId));
    }
}
