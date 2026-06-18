package com.rideflow.driver.api.dto.response;

import com.rideflow.driver.domain.model.Driver;
import com.rideflow.driver.domain.model.DriverAvailability;
import com.rideflow.driver.domain.model.GeoPoint;
import com.rideflow.driver.domain.model.VehicleType;

import java.time.Instant;
import java.util.UUID;

/**
 * Public projection of a {@link Driver}. Deliberately omits {@code passwordHash}
 * — the credential never leaves the service.
 */
public record DriverResponse(
        UUID               id,
        String             email,
        String             phone,
        String             fullName,
        VehicleType        vehicleType,
        String             vehiclePlate,
        DriverAvailability availability,
        Double             lastLat,
        Double             lastLng,
        Instant            lastLocationAt,
        Instant            createdAt
) {

    public static DriverResponse from(Driver d) {
        GeoPoint loc = d.lastLocation();
        return new DriverResponse(
                d.id(), d.email(), d.phone(), d.fullName(),
                d.vehicleType(), d.vehiclePlate(), d.availability(),
                loc == null ? null : loc.lat(),
                loc == null ? null : loc.lng(),
                d.lastLocationAt(), d.createdAt());
    }
}
