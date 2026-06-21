package com.rideflow.pricing.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for scale-2 currency-aware {@link Money} arithmetic. */
class MoneyTest {

    @Test
    void normalisesToScaleTwoHalfUp() {
        assertThat(Money.of(new BigDecimal("1"), "INR").amount()).isEqualByComparingTo("1.00");
        assertThat(Money.of(new BigDecimal("1.005"), "INR").amount()).isEqualByComparingTo("1.01");
        assertThat(Money.of(new BigDecimal("1.004"), "INR").amount()).isEqualByComparingTo("1.00");
    }

    @Test
    void zeroFactory() {
        Money z = Money.zero("INR");
        assertThat(z.amount()).isEqualByComparingTo("0.00");
        assertThat(z.currency()).isEqualTo("INR");
    }

    @Test
    void addsSameCurrency() {
        Money sum = Money.of(new BigDecimal("10.50"), "INR").add(Money.of(new BigDecimal("4.25"), "INR"));
        assertThat(sum.amount()).isEqualByComparingTo("14.75");
        assertThat(sum.currency()).isEqualTo("INR");
    }

    @Test
    void multiplyByDimensionlessFactor_thenRoundsToScaleTwo() {
        // 10.00 * 1.234 = 12.34 (12.340 -> 12.34)
        assertThat(Money.of(new BigDecimal("10.00"), "INR").multiply(new BigDecimal("1.234")).amount())
                .isEqualByComparingTo("12.34");
        // rounding: 10.00 * 0.125 = 1.250 -> 1.25 ; 10.00 * 0.1255 = 1.255 -> 1.26 (HALF_UP)
        assertThat(Money.of(new BigDecimal("10.00"), "INR").multiply(new BigDecimal("0.1255")).amount())
                .isEqualByComparingTo("1.26");
    }

    @Test
    void maxReturnsLargerAndIsCurrencyChecked() {
        Money a = Money.of(new BigDecimal("80.00"), "INR");
        Money b = Money.of(new BigDecimal("65.00"), "INR");
        assertThat(a.max(b)).isEqualTo(a);
        assertThat(b.max(a)).isEqualTo(a);
    }

    @Test
    void maxOnEqualValues_returnsReceiver() {
        Money a = Money.of(new BigDecimal("50.00"), "INR");
        Money b = Money.of(new BigDecimal("50.00"), "INR");
        // amount.compareTo >= 0 => returns 'this'
        assertThat(a.max(b)).isSameAs(a);
    }

    @Test
    void addRejectsCurrencyMismatch() {
        assertThatThrownBy(() -> Money.of(BigDecimal.ONE, "INR").add(Money.of(BigDecimal.ONE, "USD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency mismatch");
    }

    @Test
    void maxRejectsCurrencyMismatch() {
        assertThatThrownBy(() -> Money.of(BigDecimal.ONE, "INR").max(Money.of(BigDecimal.ONE, "USD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency mismatch");
    }

    @Test
    void nullAmountOrCurrency_rejected() {
        assertThatThrownBy(() -> Money.of(null, "INR")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.of(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
    }
}
