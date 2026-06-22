package com.rideflow.payment.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Wire shape of the {@code pricing.FareQuoted} payload as payment-service
 * consumes it. Only the fields needed to open a payment are read; unknown
 * fields ignored (forward-compat).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FareQuotedPayloadDto(
        UUID       quoteId,
        UUID       rideId,
        UUID       riderId,
        String     currency,
        BigDecimal total) {
}
