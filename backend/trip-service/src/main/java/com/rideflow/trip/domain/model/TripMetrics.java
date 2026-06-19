package com.rideflow.trip.domain.model;

/**
 * Actuals reported by the driver at trip completion. Carried on
 * {@code ride.completed} so a downstream final-fare pass can reconcile against
 * the up-front estimate.
 */
public record TripMetrics(int finalDistanceMeters, int finalDurationSeconds) {

    public TripMetrics {
        if (finalDistanceMeters < 0)  throw new IllegalArgumentException("finalDistanceMeters must be >= 0");
        if (finalDurationSeconds < 0) throw new IllegalArgumentException("finalDurationSeconds must be >= 0");
    }
}
