package com.rideflow.payment.domain.model;

import com.rideflow.payment.domain.exception.IllegalPaymentTransitionException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The payment aggregate — the single source of truth for a ride's settlement.
 *
 * <p>Opened in {@link PaymentStatus#PENDING} when the fare is quoted, then driven
 * (simulated) authorize → capture → settle when the ride completes. Immutable:
 * each transition validates the current state and returns a new instance.
 * Optimistic locking lives on the JPA entity's {@code @Version}, not here.
 */
public record Payment(
        UUID          id,
        UUID          rideId,
        UUID          riderId,
        UUID          driverId,
        Money         fare,
        PaymentStatus status,
        UUID          paymentMethodId,
        String        failureReason,
        Instant       createdAt,
        Instant       settledAt
) {

    public Payment {
        Objects.requireNonNull(id,      "id");
        Objects.requireNonNull(rideId,  "rideId");
        Objects.requireNonNull(riderId, "riderId");
        Objects.requireNonNull(fare,    "fare");
        Objects.requireNonNull(status,  "status");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Open a PENDING payment for a ride (consume side of pricing.fare-quoted). */
    public static Payment pending(UUID rideId, UUID riderId, Money fare, Instant when) {
        return new Payment(UUID.randomUUID(), rideId, riderId, null, fare,
                PaymentStatus.PENDING, null, null, when, null);
    }

    public Payment authorize(UUID paymentMethodId) {
        require(PaymentStatus.PENDING, "authorize");
        return copy(b -> { b.status = PaymentStatus.AUTHORIZED; b.paymentMethodId = paymentMethodId; });
    }

    public Payment capture() {
        require(PaymentStatus.AUTHORIZED, "capture");
        return copy(b -> b.status = PaymentStatus.CAPTURED);
    }

    public Payment settle(UUID driverId, Instant when) {
        require(PaymentStatus.CAPTURED, "settle");
        return copy(b -> { b.status = PaymentStatus.SETTLED; b.driverId = driverId; b.settledAt = when; });
    }

    public Payment fail(String reason) {
        if (status.isTerminal()) {
            throw new IllegalPaymentTransitionException("fail", status);
        }
        return copy(b -> { b.status = PaymentStatus.FAILED; b.failureReason = reason; });
    }

    /** Cancel (no charge) from any non-terminal state. */
    public Payment cancel(String reason) {
        if (status.isTerminal()) {
            throw new IllegalPaymentTransitionException("cancel", status);
        }
        return copy(b -> { b.status = PaymentStatus.CANCELLED; b.failureReason = reason; });
    }

    private void require(PaymentStatus expected, String action) {
        if (this.status != expected) {
            throw new IllegalPaymentTransitionException(action, status);
        }
    }

    // ------------------------------------------------------------------
    // Copy-on-write helper (same idiom as Trip): a small mutable builder
    // keeps transition methods one-liners.
    // ------------------------------------------------------------------
    private Payment copy(java.util.function.Consumer<Mutator> mutation) {
        Mutator m = new Mutator(this);
        mutation.accept(m);
        return m.build();
    }

    private static final class Mutator {
        UUID id, rideId, riderId, driverId, paymentMethodId;
        Money fare;
        PaymentStatus status;
        String failureReason;
        Instant createdAt, settledAt;

        Mutator(Payment p) {
            this.id = p.id; this.rideId = p.rideId; this.riderId = p.riderId; this.driverId = p.driverId;
            this.fare = p.fare; this.status = p.status; this.paymentMethodId = p.paymentMethodId;
            this.failureReason = p.failureReason; this.createdAt = p.createdAt; this.settledAt = p.settledAt;
        }

        Payment build() {
            return new Payment(id, rideId, riderId, driverId, fare, status,
                    paymentMethodId, failureReason, createdAt, settledAt);
        }
    }
}
