package com.rideflow.trip.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted on {@code ride.cancelled} when a rider or driver cancels a live trip.
 * The leading fields mirror {@code RideLifecyclePayloadDto} (so lenient
 * consumers read it uniformly); {@code cancelledBy}/{@code reason} are additive.
 */
public record RideCancelled(
        UUID    rideId,
        UUID    riderId,
        UUID    driverId,
        Instant occurredAt,
        String  cancelledBy,
        String  reason
) {}
