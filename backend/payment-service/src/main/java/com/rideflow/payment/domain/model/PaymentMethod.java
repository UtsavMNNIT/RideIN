package com.rideflow.payment.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A mock saved card. No real PSP integration — {@code token} is an opaque
 * demo value. {@code last4}/{@code brand} are display metadata.
 */
public record PaymentMethod(
        UUID    id,
        UUID    userId,
        String  brand,
        String  last4,
        String  token,
        boolean isDefault,
        Instant createdAt
) {

    public PaymentMethod {
        Objects.requireNonNull(id,     "id");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(brand,  "brand");
        Objects.requireNonNull(last4,  "last4");
        Objects.requireNonNull(token,  "token");
    }

    /** Mint a new mock method (fresh id + token + createdAt). */
    public static PaymentMethod create(UUID userId, String brand, String last4, boolean isDefault) {
        return new PaymentMethod(
                UUID.randomUUID(), userId, brand, last4,
                "tok_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                isDefault, Instant.now());
    }
}
