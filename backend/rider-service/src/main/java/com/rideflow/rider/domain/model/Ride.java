package com.rideflow.rider.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A ride request and its lifecycle state. Created in {@link RideStatus#REQUESTED}
 * by the rider; downstream states are projected from events in a later slice.
 */
public record Ride(
        UUID        id,
        UUID        riderId,
        GeoPoint    pickup,
        GeoPoint    dropoff,
        VehicleType vehicleType,
        RideStatus  status,
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
        return new Ride(
                UUID.randomUUID(),
                riderId,
                pickup,
                dropoff,
                vehicleType,
                RideStatus.REQUESTED,
                now,
                now,
                now
        );
    }
}
