package com.rideflow.rider.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Shape of {@code ride.started/completed/cancelled} payloads (trip-service). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RideLifecyclePayloadDto(
        UUID    rideId,
        UUID    riderId,
        UUID    driverId,
        Integer distanceMeters,
        Integer durationSeconds) {
}
