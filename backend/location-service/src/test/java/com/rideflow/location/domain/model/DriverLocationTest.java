package com.rideflow.location.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriverLocationTest {

    private static DriverLocation valid(double lat, double lng) {
        return new DriverLocation(UUID.randomUUID(), lat, lng, 90.0, 12.0,
                VehicleType.STANDARD, DriverAvailability.ONLINE, Instant.now());
    }

    @Test
    void validLocation_constructs() {
        DriverLocation l = valid(12.97, 77.59);
        assertThat(l.lat()).isEqualTo(12.97);
        assertThat(l.availability()).isEqualTo(DriverAvailability.ONLINE);
    }

    @Test
    void nullableHeadingAndSpeed_allowed() {
        DriverLocation l = new DriverLocation(UUID.randomUUID(), 0.0, 0.0, null, null,
                VehicleType.XL, DriverAvailability.ON_TRIP, Instant.now());
        assertThat(l.headingDegrees()).isNull();
        assertThat(l.speedMps()).isNull();
    }

    @Test
    void nullDriverId_throws() {
        assertThatThrownBy(() -> new DriverLocation(null, 0.0, 0.0, null, null,
                VehicleType.STANDARD, DriverAvailability.ONLINE, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("driverId required");
    }

    @Test
    void latOutsideRedisGeoBound_throws() {
        assertThatThrownBy(() -> valid(86.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Redis Geo bound");
    }

    @Test
    void latOutOfWgsRange_throws() {
        assertThatThrownBy(() -> valid(91.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lat out of range");
    }

    @Test
    void headingOutOfRange_throws() {
        assertThatThrownBy(() -> new DriverLocation(UUID.randomUUID(), 0.0, 0.0, 360.0, null,
                VehicleType.STANDARD, DriverAvailability.ONLINE, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("headingDegrees");
    }

    @Test
    void negativeSpeed_throws() {
        assertThatThrownBy(() -> new DriverLocation(UUID.randomUUID(), 0.0, 0.0, null, -1.0,
                VehicleType.STANDARD, DriverAvailability.ONLINE, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("speedMps");
    }
}
