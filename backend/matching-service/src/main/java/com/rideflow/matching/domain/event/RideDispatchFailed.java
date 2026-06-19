package com.rideflow.matching.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted on {@code matching.ride-dispatch-failed} when the radius-expansion
 * ladder is exhausted without locking a driver. Carried inside
 * {@code EventEnvelope<RideDispatchFailed>}.
 *
 * <p>Consumed by rider-service (tell the rider "no cars available, retry") and
 * by analytics (supply-gap heatmaps). The downstream policy — re-queue, surge,
 * widen vehicle types — is intentionally NOT decided here.
 */
public record RideDispatchFailed(
        UUID    rideId,
        UUID    riderId,
        String  vehicleType,
        String  reason,
        int     attemptsMade,
        int     lastRadiusMeters,
        Instant failedAt
) {

    public RideDispatchFailed {
        if (rideId == null)  throw new IllegalArgumentException("rideId required");
        if (riderId == null) throw new IllegalArgumentException("riderId required");
        if (failedAt == null) throw new IllegalArgumentException("failedAt required");
    }
}
