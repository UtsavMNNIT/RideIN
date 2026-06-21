package com.rideflow.driver.support;

import com.rideflow.driver.domain.model.Driver;
import com.rideflow.driver.domain.model.DriverAvailability;
import com.rideflow.driver.domain.model.GeoPoint;
import com.rideflow.driver.domain.model.VehicleType;

import java.time.Instant;
import java.util.UUID;

/** Builders for {@link Driver} aggregates in known states. */
public final class DriverFixtures {

    private DriverFixtures() {}

    private static final Instant T0 = Instant.parse("2026-06-20T09:00:00Z");

    public static Driver offline(UUID id) {
        return at(id, DriverAvailability.OFFLINE, null);
    }

    public static Driver online(UUID id) {
        return at(id, DriverAvailability.ONLINE, new GeoPoint(12.9, 77.5));
    }

    public static Driver onTrip(UUID id) {
        return at(id, DriverAvailability.ON_TRIP, new GeoPoint(12.9, 77.5));
    }

    private static Driver at(UUID id, DriverAvailability availability, GeoPoint loc) {
        return new Driver(
                id,
                "driver@rideflow.test",
                "+911234567890",
                "Test Driver",
                "hashed:secret",
                VehicleType.STANDARD,
                "KA01AB1234",
                availability,
                loc,
                loc == null ? null : T0,
                T0,
                T0);
    }
}
