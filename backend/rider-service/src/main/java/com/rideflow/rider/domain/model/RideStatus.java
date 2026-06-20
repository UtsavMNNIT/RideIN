package com.rideflow.rider.domain.model;

/**
 * Ride lifecycle as seen by the rider. rider-service authors {@link #REQUESTED};
 * the rest are projected from downstream events (matching + trip). Mirrors the
 * Postgres {@code rider.ride_status} enum.
 */
public enum RideStatus {
    REQUESTED,
    ASSIGNED,
    STARTED,
    COMPLETED,
    CANCELLED,
    /** Dispatch exhausted the radius ladder — no driver could be matched. */
    NO_DRIVERS_FOUND;

    /** Forward rank for the happy path; terminal states are handled separately. */
    int rank() {
        return switch (this) {
            case REQUESTED -> 0;
            case ASSIGNED  -> 1;
            case STARTED   -> 2;
            case COMPLETED -> 3;
            case CANCELLED, NO_DRIVERS_FOUND -> 99;
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == NO_DRIVERS_FOUND;
    }
}
