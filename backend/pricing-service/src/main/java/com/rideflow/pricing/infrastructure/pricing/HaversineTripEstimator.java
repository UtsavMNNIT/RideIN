package com.rideflow.pricing.infrastructure.pricing;

import com.rideflow.pricing.application.port.out.TripEstimator;
import com.rideflow.pricing.domain.model.GeoPoint;
import com.rideflow.pricing.domain.model.TripEstimate;
import com.rideflow.pricing.domain.model.VehicleType;
import com.rideflow.pricing.infrastructure.config.PricingProperties;

import org.springframework.stereotype.Component;

/**
 * Cheap, dependency-free {@link TripEstimator}: straight-line (haversine)
 * distance scaled by a configured road-factor to approximate driving distance,
 * and a flat average-speed model to turn that into a duration.
 *
 * <p>Good enough for a fare estimate that's reconciled against the real distance
 * at trip end. When a routing-service ETA exists, add a {@code RoutingTripEstimator}
 * and retire this one — the port keeps the swap invisible to the fare pipeline.
 */
@Component
public class HaversineTripEstimator implements TripEstimator {

    private final double roadFactor;
    private final double avgSpeedKmph;

    public HaversineTripEstimator(PricingProperties properties) {
        this.roadFactor   = properties.estimate().roadFactor();
        this.avgSpeedKmph = properties.estimate().avgSpeedKmph();
    }

    @Override
    public TripEstimate estimate(GeoPoint pickup, GeoPoint dropoff, VehicleType vehicleType) {
        double straightLineKm = pickup.haversineKmTo(dropoff);
        double distanceKm     = straightLineKm * roadFactor;
        double durationMin    = (distanceKm / avgSpeedKmph) * 60.0;
        return new TripEstimate(distanceKm, durationMin);
    }
}
