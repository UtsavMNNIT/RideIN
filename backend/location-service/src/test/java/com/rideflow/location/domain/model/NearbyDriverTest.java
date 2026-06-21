package com.rideflow.location.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NearbyDriverTest {

    @Test
    void holdsProjectionFields() {
        UUID id = UUID.randomUUID();
        NearbyDriver d = new NearbyDriver(id, 12.97, 77.59, 850.0, VehicleType.PREMIUM);
        assertThat(d.driverId()).isEqualTo(id);
        assertThat(d.lat()).isEqualTo(12.97);
        assertThat(d.lng()).isEqualTo(77.59);
        assertThat(d.distanceMeters()).isEqualTo(850.0);
        assertThat(d.vehicleType()).isEqualTo(VehicleType.PREMIUM);
    }
}
