package com.rideflow.matching.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire shape of the {@code rider.RideRequested} payload as matching-service
 * consumes it. Intentionally a <em>separate</em> type from rider-service's
 * domain event — consumers own their read model and stay decoupled from the
 * producer's internal classes. Unknown fields are ignored (forward-compat).
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
