package com.rideflow.payment.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Wire shape shared by {@code ride.Completed} and {@code ride.Cancelled}.
 * {@code fareTotal}/{@code currency} are only populated on completion (and even
 * then trip-service leaves them null — pricing owns the fare).
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
