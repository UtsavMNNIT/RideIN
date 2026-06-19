package com.rideflow.trip.domain.model;

import com.rideflow.trip.domain.exception.IllegalTripTransitionException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The trip lifecycle aggregate — the single source of truth for a ride's state
 * after it has been assigned to a driver.
 *
 * <p>The {@link #id} IS the upstream {@code rideId}, so a redelivered
 * {@code matching.ride-assigned} collides on the primary key and is rejected by
 * the DB (idempotency by construction, on top of the processed-events ledger).
 *
 * <p>Immutable: every transition validates the current state and returns a new
 * instance. Reject reasons are constants ({@link #REASON_DRIVER_DECLINED} /
 * {@link #REASON_EXPIRED}). Optimistic locking lives on the JPA entity's
 * {@code @Version}, not here.
 */
public record Trip(
        UUID         id,
        UUID         riderId,
        UUID         driverId,
        GeoPoint     pickup,
        GeoPoint     dropoff,
        VehicleType  vehicleType,
        TripStatus   status,
        Double       matchScore,
        String       rejectReason,
        CancelledBy  cancelledBy,
        String       cancelReason,
        Integer      finalDistanceMeters,
        Integer      finalDurationSeconds,
        Instant      offeredAt,
        Instant      offerExpiresAt,
        Instant      acceptedAt,
        Instant      arrivedAt,
        Instant      startedAt,
        Instant      completedAt,
        Instant      rejectedAt,
        Instant      cancelledAt
) {

    public static final String REASON_DRIVER_DECLINED = "DRIVER_DECLINED";
    public static final String REASON_EXPIRED         = "EXPIRED";

    public Trip {
        Objects.requireNonNull(id,             "id");
        Objects.requireNonNull(riderId,        "riderId");
        Objects.requireNonNull(driverId,       "driverId");
        Objects.requireNonNull(pickup,         "pickup");
        Objects.requireNonNull(dropoff,        "dropoff");
        Objects.requireNonNull(vehicleType,    "vehicleType");
        Objects.requireNonNull(status,         "status");
        Objects.requireNonNull(offeredAt,      "offeredAt");
        Objects.requireNonNull(offerExpiresAt, "offerExpiresAt");
    }

    /** Open an offer to the assigned driver (consume side of matching.ride-assigned). */
    public static Trip offer(UUID rideId, UUID riderId, UUID driverId,
                             GeoPoint pickup, GeoPoint dropoff, VehicleType vehicleType,
                             Double matchScore, Instant offeredAt, Instant offerExpiresAt) {
        return new Trip(rideId, riderId, driverId, pickup, dropoff, vehicleType,
                TripStatus.OFFERED, matchScore,
                null, null, null, null, null,
                offeredAt, offerExpiresAt,
                null, null, null, null, null, null);
    }

    public Trip accept(Instant when) {
        require(TripStatus.OFFERED, "accept");
        return copy(b -> { b.status = TripStatus.ACCEPTED; b.acceptedAt = when; });
    }

    public Trip reject(Instant when) {
        require(TripStatus.OFFERED, "reject");
        return copy(b -> { b.status = TripStatus.REJECTED; b.rejectReason = REASON_DRIVER_DECLINED; b.rejectedAt = when; });
    }

    /** System action: the offer timed out with no driver response. */
    public Trip expire(Instant when) {
        require(TripStatus.OFFERED, "expire");
        return copy(b -> { b.status = TripStatus.EXPIRED; b.rejectReason = REASON_EXPIRED; b.rejectedAt = when; });
    }

    public Trip arrive(Instant when) {
        require(TripStatus.ACCEPTED, "mark-arrived");
        return copy(b -> { b.status = TripStatus.ARRIVED; b.arrivedAt = when; });
    }

    public Trip start(Instant when) {
        require(TripStatus.ARRIVED, "start");
        return copy(b -> { b.status = TripStatus.STARTED; b.startedAt = when; });
    }

    public Trip complete(TripMetrics metrics, Instant when) {
        require(TripStatus.STARTED, "complete");
        Objects.requireNonNull(metrics, "metrics");
        return copy(b -> {
            b.status = TripStatus.COMPLETED;
            b.completedAt = when;
            b.finalDistanceMeters = metrics.finalDistanceMeters();
            b.finalDurationSeconds = metrics.finalDurationSeconds();
        });
    }

    /** Cancellable from any non-terminal state. */
    public Trip cancel(CancelledBy by, String reason, Instant when) {
        if (status.isTerminal()) {
            throw new IllegalTripTransitionException("cancel", status);
        }
        Objects.requireNonNull(by, "cancelledBy");
        return copy(b -> {
            b.status = TripStatus.CANCELLED;
            b.cancelledBy = by;
            b.cancelReason = reason;
            b.cancelledAt = when;
        });
    }

    private void require(TripStatus expected, String action) {
        if (this.status != expected) {
            throw new IllegalTripTransitionException(action, status);
        }
    }

    // ------------------------------------------------------------------
    // Copy-on-write helper. A small mutable builder keeps the transition
    // methods one-liners without a 21-argument constructor call each time.
    // ------------------------------------------------------------------
    private Trip copy(java.util.function.Consumer<Mutator> mutation) {
        Mutator m = new Mutator(this);
        mutation.accept(m);
        return m.build();
    }

    private static final class Mutator {
        UUID id, riderId, driverId;
        GeoPoint pickup, dropoff;
        VehicleType vehicleType;
        TripStatus status;
        Double matchScore;
        String rejectReason, cancelReason;
        CancelledBy cancelledBy;
        Integer finalDistanceMeters, finalDurationSeconds;
        Instant offeredAt, offerExpiresAt, acceptedAt, arrivedAt, startedAt, completedAt, rejectedAt, cancelledAt;

        Mutator(Trip t) {
            this.id = t.id; this.riderId = t.riderId; this.driverId = t.driverId;
            this.pickup = t.pickup; this.dropoff = t.dropoff; this.vehicleType = t.vehicleType;
            this.status = t.status; this.matchScore = t.matchScore;
            this.rejectReason = t.rejectReason; this.cancelledBy = t.cancelledBy; this.cancelReason = t.cancelReason;
            this.finalDistanceMeters = t.finalDistanceMeters; this.finalDurationSeconds = t.finalDurationSeconds;
            this.offeredAt = t.offeredAt; this.offerExpiresAt = t.offerExpiresAt;
            this.acceptedAt = t.acceptedAt; this.arrivedAt = t.arrivedAt; this.startedAt = t.startedAt;
            this.completedAt = t.completedAt; this.rejectedAt = t.rejectedAt; this.cancelledAt = t.cancelledAt;
        }

        Trip build() {
            return new Trip(id, riderId, driverId, pickup, dropoff, vehicleType, status, matchScore,
                    rejectReason, cancelledBy, cancelReason, finalDistanceMeters, finalDurationSeconds,
                    offeredAt, offerExpiresAt, acceptedAt, arrivedAt, startedAt, completedAt, rejectedAt, cancelledAt);
        }
    }
}
