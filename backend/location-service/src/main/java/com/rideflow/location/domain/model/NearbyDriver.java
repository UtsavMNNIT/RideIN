package com.rideflow.location.domain.model;

import java.util.UUID;

/**
 * Read-side projection: a candidate driver returned by a proximity query.
 *
 * Distance is precomputed by Redis (`WITHDIST`) so consumers don't repeat
 * the haversine math. ETA is intentionally NOT returned here — that's a
 * matching-service concern that factors heading, speed, and route graph.
 */
public record NearbyDriver(
        UUID        driverId,
        double      lat,
        double      lng,
        double      distanceMeters,
        VehicleType vehicleType
) {}
