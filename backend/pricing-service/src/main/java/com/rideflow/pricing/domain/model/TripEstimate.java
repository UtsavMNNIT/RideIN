package com.rideflow.pricing.domain.model;

/**
 * Up-front estimate of a trip's distance and duration — the quantities the fare
 * formula prices. Produced by
 * {@link com.rideflow.pricing.application.port.out.TripEstimator}; today a
 * haversine + road-factor approximation, tomorrow a routing-service ETA. Callers
 * never see which.
 */
public record TripEstimate(double distanceKm, double durationMinutes) {

    public TripEstimate {
        if (distanceKm < 0)      throw new IllegalArgumentException("distanceKm must be >= 0");
        if (durationMinutes < 0) throw new IllegalArgumentException("durationMinutes must be >= 0");
    }
}
