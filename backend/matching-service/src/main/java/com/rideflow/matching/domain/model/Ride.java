package com.rideflow.matching.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Dispatch aggregate root. The matching service does not own the ride's full
 * lifecycle (accept/start/complete live downstream) — it owns the <em>dispatch
 * outcome</em>: did we find a driver, who, and at what score.
 *
 * <p>The id is the upstream {@code rideId} minted by rider-service, so it
 * doubles as a natural idempotency key: a redelivered {@code RideRequested}
 * collides on the primary key and is rejected by the DB.
 *
 * <p>Immutable. Transitions return a new instance; the in-memory
 * {@link RideStatus#DISPATCHING} state is transient — only the terminal outcome
 * ({@code ASSIGNED} / {@code DISPATCH_FAILED}) is persisted.
 */
public record Ride(
        UUID       id,
        UUID       riderId,
        GeoPoint   pickup,
        GeoPoint   dropoff,
        VehicleType vehicleType,
        RideStatus status,
        UUID       assignedDriverId,
        Double     assignmentScore,
        String     failureReason,
        Instant    requestedAt,
        Instant    assignedAt,
        Instant    failedAt
) {

    public Ride {
        if (id == null)          throw new IllegalArgumentException("ride id required");
        if (riderId == null)     throw new IllegalArgumentException("riderId required");
        if (pickup == null)      throw new IllegalArgumentException("pickup required");
        if (dropoff == null)     throw new IllegalArgumentException("dropoff required");
        if (vehicleType == null) throw new IllegalArgumentException("vehicleType required");
        if (status == null)      throw new IllegalArgumentException("status required");
        if (requestedAt == null) throw new IllegalArgumentException("requestedAt required");
    }

    /** Freshly received request, before any dispatch attempt. */
    public static Ride requested(UUID id, UUID riderId, GeoPoint pickup, GeoPoint dropoff,
                                 VehicleType vehicleType, Instant requestedAt) {
        return new Ride(id, riderId, pickup, dropoff, vehicleType,
                RideStatus.REQUESTED, null, null, null, requestedAt, null, null);
    }

    /** Enter the radius-expansion loop. */
    public Ride beginDispatch() {
        requireFrom(RideStatus.REQUESTED);
        return withStatus(RideStatus.DISPATCHING);
    }

    /** A driver was locked and won scoring. */
    public Ride assignTo(UUID driverId, double score, Instant when) {
        if (driverId == null) throw new IllegalArgumentException("driverId required to assign");
        return new Ride(id, riderId, pickup, dropoff, vehicleType,
                RideStatus.ASSIGNED, driverId, score, null, requestedAt, when, null);
    }

    /** No driver could be matched within the radius ladder. */
    public Ride fail(String reason, Instant when) {
        return new Ride(id, riderId, pickup, dropoff, vehicleType,
                RideStatus.DISPATCH_FAILED, null, null, reason, requestedAt, null, when);
    }

    private Ride withStatus(RideStatus next) {
        return new Ride(id, riderId, pickup, dropoff, vehicleType,
                next, assignedDriverId, assignmentScore, failureReason,
                requestedAt, assignedAt, failedAt);
    }

    private void requireFrom(RideStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    "illegal transition from " + status + " (expected " + expected + ")");
        }
    }
}
