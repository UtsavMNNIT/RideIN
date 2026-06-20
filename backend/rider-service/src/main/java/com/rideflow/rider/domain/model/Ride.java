package com.rideflow.rider.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A ride request and its lifecycle state, as the rider sees it.
 *
 * <p>rider-service authors {@link RideStatus#REQUESTED}; all later state is a
 * <em>read projection</em> built by consuming downstream events
 * (matching.ride-assigned / dispatch-failed, ride.started/completed/cancelled,
 * pricing.fare-quoted). The {@code markXxx} transitions are deliberately
 * <b>monotonic</b>: they only ever move the status forward (or to a terminal
 * state) and ignore stale/out-of-order or duplicate events, because cross-topic
 * delivery offers no global ordering.
 */
public record Ride(
        UUID        id,
        UUID        riderId,
        GeoPoint    pickup,
        GeoPoint    dropoff,
        VehicleType vehicleType,
        RideStatus  status,
        UUID        assignedDriverId,
        Double      matchScore,
        BigDecimal  fareTotal,
        String      currency,
        Integer     finalDistanceMeters,
        Integer     finalDurationSeconds,
        Instant     requestedAt,
        Instant     createdAt,
        Instant     updatedAt
) {

    public Ride {
        Objects.requireNonNull(id,          "id");
        Objects.requireNonNull(riderId,     "riderId");
        Objects.requireNonNull(pickup,      "pickup");
        Objects.requireNonNull(dropoff,     "dropoff");
        Objects.requireNonNull(vehicleType, "vehicleType");
        Objects.requireNonNull(status,      "status");
        Objects.requireNonNull(requestedAt, "requestedAt");
        Objects.requireNonNull(createdAt,   "createdAt");
        Objects.requireNonNull(updatedAt,   "updatedAt");
    }

    /** Factory for a brand-new ride request. */
    public static Ride request(UUID riderId, GeoPoint pickup, GeoPoint dropoff, VehicleType vehicleType) {
        Instant now = Instant.now();
        return new Ride(UUID.randomUUID(), riderId, pickup, dropoff, vehicleType,
                RideStatus.REQUESTED, null, null, null, null, null, null, now, now, now);
    }

    // ------------------------------------------------------------------
    // Monotonic projection transitions. Each returns a new instance (or
    // {@code this} when the event is stale/duplicate and should be ignored).
    // ------------------------------------------------------------------

    public Ride markAssigned(UUID driverId, Double score) {
        if (status != RideStatus.REQUESTED) return this;     // already past offer, or terminal
        return copy(b -> { b.status = RideStatus.ASSIGNED; b.assignedDriverId = driverId; b.matchScore = score; });
    }

    public Ride markNoDrivers() {
        if (status != RideStatus.REQUESTED) return this;
        return copy(b -> b.status = RideStatus.NO_DRIVERS_FOUND);
    }

    public Ride markStarted() {
        if (status.isTerminal() || status.rank() >= RideStatus.STARTED.rank()) return this;
        return copy(b -> b.status = RideStatus.STARTED);
    }

    public Ride markCompleted(Integer distanceMeters, Integer durationSeconds) {
        if (status.isTerminal() || status.rank() >= RideStatus.COMPLETED.rank()) return this;
        return copy(b -> {
            b.status = RideStatus.COMPLETED;
            b.finalDistanceMeters = distanceMeters;
            b.finalDurationSeconds = durationSeconds;
        });
    }

    public Ride markCancelled() {
        if (status.isTerminal()) return this;
        return copy(b -> b.status = RideStatus.CANCELLED);
    }

    /** Attach the quoted fare. Independent of lifecycle status; idempotent. */
    public Ride withFare(BigDecimal total, String currency) {
        return copy(b -> { b.fareTotal = total; b.currency = currency; });
    }

    private Ride copy(java.util.function.Consumer<Mutator> mutation) {
        Mutator m = new Mutator(this);
        mutation.accept(m);
        m.updatedAt = Instant.now();
        return m.build();
    }

    private static final class Mutator {
        UUID id, riderId, assignedDriverId;
        GeoPoint pickup, dropoff;
        VehicleType vehicleType;
        RideStatus status;
        Double matchScore;
        BigDecimal fareTotal;
        String currency;
        Integer finalDistanceMeters, finalDurationSeconds;
        Instant requestedAt, createdAt, updatedAt;

        Mutator(Ride r) {
            this.id = r.id; this.riderId = r.riderId; this.assignedDriverId = r.assignedDriverId;
            this.pickup = r.pickup; this.dropoff = r.dropoff; this.vehicleType = r.vehicleType;
            this.status = r.status; this.matchScore = r.matchScore;
            this.fareTotal = r.fareTotal; this.currency = r.currency;
            this.finalDistanceMeters = r.finalDistanceMeters; this.finalDurationSeconds = r.finalDurationSeconds;
            this.requestedAt = r.requestedAt; this.createdAt = r.createdAt; this.updatedAt = r.updatedAt;
        }

        Ride build() {
            return new Ride(id, riderId, pickup, dropoff, vehicleType, status, assignedDriverId, matchScore,
                    fareTotal, currency, finalDistanceMeters, finalDurationSeconds, requestedAt, createdAt, updatedAt);
        }
    }
}
