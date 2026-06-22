package com.rideflow.payment.api.dto.response;

import com.rideflow.payment.domain.model.PaymentMethod;

import java.time.Instant;
import java.util.UUID;

/** A mock saved card as returned by the payment API (token never exposed). */
public record PaymentMethodResponse(
        UUID    id,
        UUID    userId,
        String  brand,
        String  last4,
        boolean isDefault,
        Instant createdAt) {

    public static PaymentMethodResponse from(PaymentMethod m) {
        return new PaymentMethodResponse(
                m.id(), m.userId(), m.brand(), m.last4(), m.isDefault(), m.createdAt());
    }
}
