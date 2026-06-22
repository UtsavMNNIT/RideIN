package com.rideflow.payment.domain.model;

/**
 * Payment lifecycle states.
 *
 * <pre>
 *   PENDING   → AUTHORIZED | FAILED | CANCELLED
 *   AUTHORIZED→ CAPTURED   | FAILED | CANCELLED
 *   CAPTURED  → SETTLED    | FAILED
 *   SETTLED / FAILED / CANCELLED = terminal
 * </pre>
 */
public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    SETTLED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == SETTLED || this == FAILED || this == CANCELLED;
    }
}
