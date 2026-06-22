package com.rideflow.payment.api.dto.response;

import com.rideflow.payment.domain.model.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A payment / receipt as returned by the payment API. */
public record PaymentResponse(
        UUID       paymentId,
        UUID       rideId,
        UUID       riderId,
        UUID       driverId,
        BigDecimal amount,
        String     currency,
        String     status,
        UUID       paymentMethodId,
        String     failureReason,
        Instant    createdAt,
        Instant    settledAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.id(), p.rideId(), p.riderId(), p.driverId(),
                p.fare().amount(), p.fare().currency(), p.status().name(),
                p.paymentMethodId(), p.failureReason(), p.createdAt(), p.settledAt());
    }
}
