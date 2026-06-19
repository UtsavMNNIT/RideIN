package com.rideflow.trip.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Wire shape of the {@code matching.RideAssigned} payload as trip-service
 * consumes it. Owned by this service (decoupled from matching's internals);
 * unknown fields ignored (forward-compat).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RideAssignedPayloadDto(
        UUID   rideId,
        UUID   riderId,
        UUID   driverId,
        double pickupLat,
        double pickupLng,
        double dropoffLat,
        double dropoffLng,
        String vehicleType,
        Double matchScore) {
}
