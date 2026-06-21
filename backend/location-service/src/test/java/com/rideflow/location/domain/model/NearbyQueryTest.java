package com.rideflow.location.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NearbyQueryTest {

    @Test
    void validQuery_constructs() {
        NearbyQuery q = new NearbyQuery(12.97, 77.59, 3000, VehicleType.STANDARD, 10);
        assertThat(q.lat()).isEqualTo(12.97);
        assertThat(q.lng()).isEqualTo(77.59);
        assertThat(q.radiusMeters()).isEqualTo(3000);
        assertThat(q.vehicleType()).isEqualTo(VehicleType.STANDARD);
        assertThat(q.limit()).isEqualTo(10);
    }

    @Test
    void latOutOfRange_throws() {
        assertThatThrownBy(() -> new NearbyQuery(91.0, 0.0, 100, VehicleType.STANDARD, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lat out of range");
    }

    @Test
    void lngOutOfRange_throws() {
        assertThatThrownBy(() -> new NearbyQuery(0.0, 181.0, 100, VehicleType.STANDARD, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lng out of range");
    }

    @Test
    void nonPositiveRadius_throws() {
        assertThatThrownBy(() -> new NearbyQuery(0.0, 0.0, 0, VehicleType.STANDARD, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("radiusMeters must be positive");
    }

    @Test
    void nullVehicleType_throws() {
        assertThatThrownBy(() -> new NearbyQuery(0.0, 0.0, 100, null, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vehicleType required");
    }

    @Test
    void nonPositiveLimit_throws() {
        assertThatThrownBy(() -> new NearbyQuery(0.0, 0.0, 100, VehicleType.STANDARD, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit must be positive");
    }
}
