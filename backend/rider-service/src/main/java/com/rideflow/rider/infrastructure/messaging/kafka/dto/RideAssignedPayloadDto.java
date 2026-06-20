package com.rideflow.rider.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Subset of {@code matching.RideAssigned} the rider projection needs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RideAssignedPayloadDto(
        UUID   rideId,
        UUID   riderId,
        UUID   driverId,
        Double matchScore) {
}
