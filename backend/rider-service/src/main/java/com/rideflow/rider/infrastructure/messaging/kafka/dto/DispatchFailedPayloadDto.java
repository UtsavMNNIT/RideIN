package com.rideflow.rider.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Subset of {@code matching.RideDispatchFailed} the rider projection needs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DispatchFailedPayloadDto(
        UUID rideId,
        UUID riderId) {
}
