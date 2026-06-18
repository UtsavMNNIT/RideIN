package com.rideflow.location.domain.model;

/**
 * Value object for a "find nearby drivers" query.
 *
 * Bounds (radius cap, limit cap) are enforced at the API boundary so the
 * domain remains a pure VO; the use-case re-validates defensively.
 */
public record NearbyQuery(
        double      lat,
        double      lng,
        int         radiusMeters,
        VehicleType vehicleType,
        int         limit
) {

    public NearbyQuery {
        if (lat < -90.0 || lat > 90.0)
            throw new IllegalArgumentException("lat out of range: " + lat);
        if (lng < -180.0 || lng > 180.0)
            throw new IllegalArgumentException("lng out of range: " + lng);
        if (radiusMeters <= 0)
            throw new IllegalArgumentException("radiusMeters must be positive: " + radiusMeters);
        if (vehicleType == null)
            throw new IllegalArgumentException("vehicleType required");
        if (limit <= 0)
            throw new IllegalArgumentException("limit must be positive: " + limit);
    }
}
