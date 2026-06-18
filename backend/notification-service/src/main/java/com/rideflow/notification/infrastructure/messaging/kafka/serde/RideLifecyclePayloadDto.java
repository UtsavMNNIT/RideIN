package com.rideflow.notification.infrastructure.messaging.kafka.serde;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Wire shape shared by {@code ride.Started} and {@code ride.Completed}.
 * {@code distanceMeters}, {@code durationSeconds}, {@code fareTotal} and
 * {@code currency} are only populated on completion — null on start.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RideLifecyclePayloadDto(
        UUID       rideId,
        UUID       riderId,
        UUID       driverId,
        Instant    occurredAt,
        Integer    distanceMeters,
        Integer    durationSeconds,
        BigDecimal fareTotal,
        String     currency
) {}
