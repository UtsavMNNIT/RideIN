package com.rideflow.driver.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumer-side view of the ride-lifecycle payloads driver-service cares about
 * ({@code ride.accepted/completed/cancelled}). Only {@code driverId} is required
 * to sync availability; other fields are read leniently and may be absent.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RideLifecyclePayloadDto(
        UUID    rideId,
        UUID    riderId,
        UUID    driverId,
        Instant occurredAt,
        String  cancelledBy) {
}
