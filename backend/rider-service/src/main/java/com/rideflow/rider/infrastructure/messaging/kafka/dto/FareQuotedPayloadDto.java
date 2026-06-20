package com.rideflow.rider.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

/** Subset of {@code pricing.FareQuoted} the rider projection needs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FareQuotedPayloadDto(
        UUID       rideId,
        UUID       riderId,
        BigDecimal total,
        String     currency) {
}
