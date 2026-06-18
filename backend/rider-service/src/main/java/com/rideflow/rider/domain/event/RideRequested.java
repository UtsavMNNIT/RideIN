package com.rideflow.rider.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event payload published when a rider requests a ride. Carried inside
 * {@code EventEnvelope<RideRequested>} on topic {@code rider.ride-requested},
 * consumed by matching-service (and pricing for a fare quote).
 *
 * <p>Coordinates are flattened (not a nested object) to keep the wire schema
 * stable and trivially consumable across services. Enum fields are strings to
 * keep consumer coupling loose.
 *
 * <p>Schema version 1 — additive changes only; never remove/rename a field
 * without bumping the major version and forking the topic.
 */
public record RideRequested(
        UUID    rideId,
        UUID    riderId,
        double  pickupLat,
        double  pickupLng,
        double  dropoffLat,
        double  dropoffLng,
        String  vehicleType,
        Instant requestedAt
) {

    public RideRequested {
        if (rideId == null)     throw new IllegalArgumentException("rideId required");
        if (riderId == null)    throw new IllegalArgumentException("riderId required");
        if (vehicleType == null || vehicleType.isBlank())
            throw new IllegalArgumentException("vehicleType required");
        if (requestedAt == null) throw new IllegalArgumentException("requestedAt required");
    }
}
