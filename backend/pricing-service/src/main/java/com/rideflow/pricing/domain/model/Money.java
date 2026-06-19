package com.rideflow.pricing.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Currency-aware monetary value. Always scale-2, {@code HALF_UP} — money is
 * {@link BigDecimal}, never {@code double}, so fares never drift by a rounding
 * cent. Arithmetic between mismatched currencies is rejected loudly.
 */
public record Money(BigDecimal amount, String currency) {

    private static final int SCALE = 2;

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), currency);
    }

    /** Multiply by a dimensionless factor (e.g. distance, surge). */
    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor), currency);
    }

    public Money max(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0 ? this : other;
    }

    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "currency mismatch: " + currency + " vs " + other.currency);
        }
    }
}
