package com.rideflow.notification.infrastructure.messaging.kafka.serde;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Wire shape of the {@code payment.Settled} payload as notification consumes it. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentSettledPayloadDto(
        UUID       paymentId,
        UUID       rideId,
        UUID       riderId,
        UUID       driverId,
        BigDecimal amount,
        String     currency,
        String     status,
        Instant    settledAt
) {}
