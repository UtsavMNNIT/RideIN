package com.rideflow.trip.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Emitted on {@code ride.started} when the trip begins.
 *
 * <p>Field shape intentionally matches notification-service's
 * {@code RideLifecyclePayloadDto} so its existing consumer needs no change.
 * Distance/duration/fare are {@code null} at start (set only on completion);
 * {@code @JsonInclude(NON_NULL)} on the envelope drops them from the wire.
 */
public record RideStarted(
        UUID       rideId,
        UUID       riderId,
        UUID       driverId,
        Instant    occurredAt,
        Integer    distanceMeters,
        Integer    durationSeconds,
        BigDecimal fareTotal,
        String     currency
) {
    public static RideStarted of(UUID rideId, UUID riderId, UUID driverId, Instant occurredAt) {
        return new RideStarted(rideId, riderId, driverId, occurredAt, null, null, null, null);
    }
}
