package com.rideflow.location.api.dto.response;

import com.rideflow.location.domain.model.NearbyDriver;

import java.util.UUID;

/**
 * REST response shape for one nearby driver entry.
 *
 * Distance is rounded to whole metres at the boundary — the client doesn't
 * benefit from sub-metre precision, and it keeps the JSON terse.
 */
public record NearbyDriverResponse(
        UUID   driverId,
        double lat,
        double lng,
        long   distanceMeters,
        String vehicleType
) {

    public static NearbyDriverResponse from(NearbyDriver d) {
        return new NearbyDriverResponse(
                d.driverId(),
                d.lat(),
                d.lng(),
                Math.round(d.distanceMeters()),
                d.vehicleType().name()
        );
    }
}
