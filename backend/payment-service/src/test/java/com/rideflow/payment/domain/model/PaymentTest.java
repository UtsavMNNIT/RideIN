package com.rideflow.payment.domain.model;

import com.rideflow.payment.domain.exception.IllegalPaymentTransitionException;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private static Payment pending() {
        return Payment.pending(UUID.randomUUID(), UUID.randomUUID(),
                Money.of(new BigDecimal("142.00"), "INR"), Instant.now());
    }

    @Test
    void authorizeCaptureSettleHappyPath() {
        UUID driver = UUID.randomUUID();
        Instant now = Instant.now();

        Payment settled = pending().authorize(null).capture().settle(driver, now);

        assertThat(settled.status()).isEqualTo(PaymentStatus.SETTLED);
        assertThat(settled.driverId()).isEqualTo(driver);
        assertThat(settled.settledAt()).isEqualTo(now);
        assertThat(settled.status().isTerminal()).isTrue();
    }

    @Test
    void cannotSettleFromPending() {
        Payment p = pending();
        assertThatThrownBy(() -> p.settle(UUID.randomUUID(), Instant.now()))
                .isInstanceOf(IllegalPaymentTransitionException.class);
    }

    @Test
    void cannotCancelTerminalPayment() {
        Payment settled = pending().authorize(null).capture().settle(UUID.randomUUID(), Instant.now());
        assertThatThrownBy(() -> settled.cancel("late"))
                .isInstanceOf(IllegalPaymentTransitionException.class);
    }

    @Test
    void canCancelPendingPayment() {
        Payment cancelled = pending().cancel("ride cancelled");
        assertThat(cancelled.status()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void canFailFromAnyNonTerminalState() {
        Payment failed = pending().authorize(null).fail("declined");
        assertThat(failed.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failed.failureReason()).isEqualTo("declined");
    }
}
