package com.rideflow.matching.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire shape of the {@code ride.rejected} payload (produced by trip-service when
 * the offered driver declines or the offer expires). Intentionally a separate
 * type from trip-service's domain event — consumers own their read model.
 * Unknown fields are ignored (forward-compat).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RideRejectedPayloadDto(
        UUID    rideId,
        UUID    riderId,
        UUID    driverId,
        String  reason,
        Instant rejectedAt) {
}
