package com.rideflow.notification.infrastructure.messaging.kafka.serde;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire shape for the {@code matching.RideAssigned} payload. Mirror of the
 * producer's contract (matching-service); kept local to this service until a
 * cross-service contract module ships in a later phase.
 *
 * <p>Lenient deserialization ({@code ignoreUnknown=true}) makes the consumer
 * forward-compatible with additive producer fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RideAssignedPayloadDto(
        UUID    rideId,
        UUID    riderId,
        UUID    driverId,
        String  vehicleType,
        Double  pickupLat,
        Double  pickupLng,
        Double  dropoffLat,
        Double  dropoffLng,
        Integer etaSeconds,
        Instant assignedAt
) {}
