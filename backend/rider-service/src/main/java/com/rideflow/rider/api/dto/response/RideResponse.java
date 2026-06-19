package com.rideflow.rider.api.dto.response;

import com.rideflow.rider.domain.model.Ride;
import com.rideflow.rider.domain.model.RideStatus;
import com.rideflow.rider.domain.model.VehicleType;

import java.time.Instant;
import java.util.UUID;

/**
 * Public projection of a {@link Ride}, used for both the request response and
 * ride-history entries.
 */
public record RideResponse(
        UUID        id,
        UUID        riderId,
        double      pickupLat,
        double      pickupLng,
        double      dropoffLat,
        double      dropoffLng,
        VehicleType vehicleType,
        RideStatus  status,
        Instant     requestedAt
) {

    public static RideResponse from(Ride r) {
        return new RideResponse(
                r.id(), r.riderId(),
                r.pickup().lat(),  r.pickup().lng(),
                r.dropoff().lat(), r.dropoff().lng(),
                r.vehicleType(), r.status(), r.requestedAt());
    }
}
