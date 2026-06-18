package com.rideflow.rider.domain.model;

/**
 * Ride lifecycle as seen by the rider. rider-service authors {@link #REQUESTED};
 * later states are projected from downstream events. Mirrors the Postgres
 * {@code rider.ride_status} enum.
 */
public enum RideStatus {
    REQUESTED,
    ASSIGNED,
    STARTED,
    COMPLETED,
    CANCELLED
}
