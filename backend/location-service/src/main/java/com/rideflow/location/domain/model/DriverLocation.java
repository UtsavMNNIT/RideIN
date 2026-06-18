package com.rideflow.location.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain write-model for a driver's latest reported location.
 *
 * Invariants enforced in the compact constructor — if an upstream event
 * violates them, the consumer drops the record to DLQ rather than corrupting
 * the geo index.
 *
 * Coordinate convention: {@code lat} is the geographic latitude, {@code lng}
 * the longitude. Redis Geo internally orders as (lng, lat); the repository
 * adapter is responsible for the swap.
 */
public record DriverLocation(
        UUID               driverId,
        double             lat,
        double             lng,
        Double             headingDegrees,    // nullable
        Double             speedMps,          // nullable
        VehicleType        vehicleType,
        DriverAvailability availability,
        Instant            capturedAt
) {

    public DriverLocation {
        if (driverId == null)              throw new IllegalArgumentException("driverId required");
        if (vehicleType == null)           throw new IllegalArgumentException("vehicleType required");
        if (availability == null)          throw new IllegalArgumentException("availability required");
        if (capturedAt == null)            throw new IllegalArgumentException("capturedAt required");
        if (lat < -90.0 || lat > 90.0)     throw new IllegalArgumentException("lat out of range: " + lat);
        if (lng < -180.0 || lng > 180.0)   throw new IllegalArgumentException("lng out of range: " + lng);
        // Redis Geo has a stricter latitude bound (Mercator projection limit).
        if (lat < -85.05112878 || lat > 85.05112878)
            throw new IllegalArgumentException("lat outside Redis Geo bound: " + lat);
        if (headingDegrees != null && (headingDegrees < 0 || headingDegrees >= 360))
            throw new IllegalArgumentException("headingDegrees must be [0,360): " + headingDegrees);
        if (speedMps != null && speedMps < 0)
            throw new IllegalArgumentException("speedMps must be non-negative: " + speedMps);
    }
}
