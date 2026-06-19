package com.rideflow.trip.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Emitted on {@code ride.accepted} when the offered driver accepts the trip. */
public record RideAccepted(
        UUID    rideId,
        UUID    riderId,
        UUID    driverId,
        Instant acceptedAt
) {}
