package com.rideflow.trip.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted on {@code ride.rejected} when the offered driver declines or the offer
 * expires. {@code reason} is {@code DRIVER_DECLINED} or {@code EXPIRED}. No
 * re-dispatch this iteration — the rider re-requests.
 */
public record RideRejected(
        UUID    rideId,
        UUID    riderId,
        UUID    driverId,
        String  reason,
        Instant rejectedAt
) {}
