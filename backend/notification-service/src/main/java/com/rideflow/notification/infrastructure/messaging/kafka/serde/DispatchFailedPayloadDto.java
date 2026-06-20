package com.rideflow.notification.infrastructure.messaging.kafka.serde;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire shape for the {@code matching.RideDispatchFailed} payload. Mirror of the
 * producer's contract (matching-service); kept local to this service.
 *
 * <p>Note there is no {@code driverId} — dispatch failed precisely because no
 * driver could be matched.
 *
 * <p>Lenient deserialization ({@code ignoreUnknown=true}) makes the consumer
 * forward-compatible with additive producer fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DispatchFailedPayloadDto(
        UUID    rideId,
        UUID    riderId,
        String  vehicleType,
        String  reason,
        Integer attemptsMade,
        Integer lastRadiusMeters,
        Instant failedAt
) {}
