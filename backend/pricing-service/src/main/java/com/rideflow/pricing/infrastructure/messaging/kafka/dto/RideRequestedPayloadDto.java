package com.rideflow.pricing.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire shape of the {@code rider.RideRequested} payload as pricing-service
 * consumes it. A separate type from rider-service's domain event — the consumer
 * owns its read model and stays decoupled from the producer's internals. Unknown
 * fields are ignored (forward-compat).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RideRequestedPayloadDto(
        UUID    rideId,
        UUID    riderId,
        double  pickupLat,
        double  pickupLng,
        double  dropoffLat,
        double  dropoffLng,
        String  vehicleType,
        Instant requestedAt) {
}
