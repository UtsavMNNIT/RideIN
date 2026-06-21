package com.rideflow.pricing.infrastructure.pricing;

import com.rideflow.pricing.domain.model.GeoPoint;
import com.rideflow.pricing.domain.model.TripEstimate;
import com.rideflow.pricing.domain.model.VehicleType;
import com.rideflow.pricing.infrastructure.config.PricingProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Unit tests for the haversine + road-factor trip estimator. */
class HaversineTripEstimatorTest {

    private static PricingProperties props(double roadFactor, double avgSpeedKmph) {
        return new PricingProperties(
                "INR",
                new PricingProperties.Estimate(roadFactor, avgSpeedKmph),
                new PricingProperties.Quote(5),
                new PricingProperties.Surge(new BigDecimal("1.00"), Map.of(), null));
    }

    @Test
    void zeroDistanceWhenPickupEqualsDropoff() {
        HaversineTripEstimator est = new HaversineTripEstimator(props(1.3, 30.0));
        GeoPoint p = new GeoPoint(12.97, 77.59);
        TripEstimate e = est.estimate(p, p, VehicleType.STANDARD);
        assertThat(e.distanceKm()).isEqualTo(0.0);
        assertThat(e.durationMinutes()).isEqualTo(0.0);
    }

    @Test
    void appliesRoadFactorToStraightLineDistance() {
        // One degree of latitude ~= 111.19 km along a meridian.
        GeoPoint a = new GeoPoint(0.0, 0.0);
        GeoPoint b = new GeoPoint(1.0, 0.0);
        double straightLine = a.haversineKmTo(b);

        HaversineTripEstimator est = new HaversineTripEstimator(props(1.4, 40.0));
        TripEstimate e = est.estimate(a, b, VehicleType.STANDARD);

        assertThat(straightLine).isCloseTo(111.19, within(0.5));
        assertThat(e.distanceKm()).isCloseTo(straightLine * 1.4, within(1e-9));
        // duration = (distanceKm / 40) * 60
        assertThat(e.durationMinutes()).isCloseTo((straightLine * 1.4 / 40.0) * 60.0, within(1e-9));
    }

    @Test
    void distanceAndDurationAreMonotonicWithSeparation() {
        HaversineTripEstimator est = new HaversineTripEstimator(props(1.3, 30.0));
        GeoPoint origin = new GeoPoint(12.97, 77.59);
        GeoPoint near = new GeoPoint(12.98, 77.60);
        GeoPoint far = new GeoPoint(13.10, 77.75);

        TripEstimate nearEst = est.estimate(origin, near, VehicleType.STANDARD);
        TripEstimate farEst = est.estimate(origin, far, VehicleType.STANDARD);

        assertThat(farEst.distanceKm()).isGreaterThan(nearEst.distanceKm());
        assertThat(farEst.durationMinutes()).isGreaterThan(nearEst.durationMinutes());
    }

    @Test
    void durationScalesInverselyWithAvgSpeed() {
        GeoPoint a = new GeoPoint(0.0, 0.0);
        GeoPoint b = new GeoPoint(0.5, 0.5);

        TripEstimate slow = new HaversineTripEstimator(props(1.3, 20.0))
                .estimate(a, b, VehicleType.STANDARD);
        TripEstimate fast = new HaversineTripEstimator(props(1.3, 60.0))
                .estimate(a, b, VehicleType.STANDARD);

        // Same distance, 3x speed => ~1/3 duration.
        assertThat(slow.distanceKm()).isCloseTo(fast.distanceKm(), within(1e-9));
        assertThat(slow.durationMinutes()).isCloseTo(fast.durationMinutes() * 3.0, within(1e-6));
    }
}
