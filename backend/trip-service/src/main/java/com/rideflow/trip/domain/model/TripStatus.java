package com.rideflow.trip.domain.model;

/**
 * Trip lifecycle states.
 *
 * <pre>
 *   OFFERED  → ACCEPTED | REJECTED | EXPIRED | CANCELLED
 *   ACCEPTED → ARRIVED  | CANCELLED
 *   ARRIVED  → STARTED  | CANCELLED
 *   STARTED  → COMPLETED | CANCELLED
 *   COMPLETED / REJECTED / EXPIRED / CANCELLED = terminal
 * </pre>
 */
public enum TripStatus {
    OFFERED,
    ACCEPTED,
    ARRIVED,
    STARTED,
    COMPLETED,
    REJECTED,
    EXPIRED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == REJECTED || this == EXPIRED || this == CANCELLED;
    }
}
