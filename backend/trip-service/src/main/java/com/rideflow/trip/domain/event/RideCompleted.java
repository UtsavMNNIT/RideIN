package com.rideflow.trip.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Emitted on {@code ride.completed} when the trip ends.
 *
 * <p>Same wire shape as notification-service's {@code RideLifecyclePayloadDto}.
 * Carries the driver-reported actuals (distance/duration) so a future final-fare
 * pass can reconcile against the up-front estimate; {@code fareTotal}/
 * {@code currency} are left {@code null} (trip-service does not price).
 */
public record RideCompleted(
        UUID       rideId,
        UUID       riderId,
        UUID       driverId,
        Instant    occurredAt,
        Integer    distanceMeters,
        Integer    durationSeconds,
        BigDecimal fareTotal,
        String     currency
) {}
