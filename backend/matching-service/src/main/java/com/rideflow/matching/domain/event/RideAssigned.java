package com.rideflow.matching.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted on {@code matching.ride-assigned} when a driver is matched to a ride.
 * Carried inside {@code EventEnvelope<RideAssigned>}.
 *
 * <p>Consumed by driver-service (push the offer to the driver app),
 * notification-service (notify the rider), and pricing-service (lock the quote).
 *
 * <p>Coordinates are flattened and enums are strings — same wire-stability rules
 * as {@code rider.RideRequested}. Schema version 1; additive changes only.
 */
public record RideAssigned(
        UUID    rideId,
        UUID    riderId,
        UUID    driverId,
        double  pickupLat,
        double  pickupLng,
        double  dropoffLat,
        double  dropoffLng,
        String  vehicleType,
        double  matchScore,
        double  pickupDistanceMeters,
        int     dispatchAttempts,
        Instant assignedAt
) {

    public RideAssigned {
        if (rideId == null)   throw new IllegalArgumentException("rideId required");
        if (riderId == null)  throw new IllegalArgumentException("riderId required");
        if (driverId == null) throw new IllegalArgumentException("driverId required");
        if (assignedAt == null) throw new IllegalArgumentException("assignedAt required");
    }
}
