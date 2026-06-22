package com.rideflow.payment.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Emitted on {@code payment.settled} once a ride's fare has been settled
 * (simulated). Carries the amount so consumers (notification, analytics) can
 * render a receipt without re-pricing. Money is a plain {@link BigDecimal} in
 * {@code currency} — the wire contract carries numbers, not the internal
 * {@code Money} type.
 */
public record PaymentSettled(
        UUID       paymentId,
        UUID       rideId,
        UUID       riderId,
        UUID       driverId,
        BigDecimal amount,
        String     currency,
        String     status,
        Instant    settledAt
) {}
