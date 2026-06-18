package com.rideflow.driver.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event payload: a single location reading from a driver's device.
 *
 * Carried inside {@code EventEnvelope<DriverLocationUpdated>} on Kafka topic
 * {@code driver.location-updated} (24 partitions, key = driverId).
 *
 * Schema version 1 — tracked in the envelope. Add new fields freely;
 * never remove or rename without bumping the major version and forking
 * the topic.
 *
 * <p>Cross-service enum fields ({@code vehicleType}, {@code availability})
 * are serialized as strings to keep consumer coupling loose — receiving
 * services map to their own enums.
 */
public record DriverLocationUpdated(
        UUID    driverId,
        double  lat,
        double  lng,
        Double  headingDegrees,   // 0..360, nullable
        Double  speedMps,         // metres/second, nullable
        String  vehicleType,      // STANDARD | XL | PREMIUM
        String  availability,     // ONLINE | ON_TRIP
        Instant capturedAt        // when the device recorded it (not server clock)
) {

    public DriverLocationUpdated {
        if (driverId == null)               throw new IllegalArgumentException("driverId required");
        if (capturedAt == null)             throw new IllegalArgumentException("capturedAt required");
        if (lat < -90.0 || lat > 90.0)      throw new IllegalArgumentException("lat out of range: " + lat);
        if (lng < -180.0 || lng > 180.0)    throw new IllegalArgumentException("lng out of range: " + lng);
        if (headingDegrees != null && (headingDegrees < 0 || headingDegrees >= 360))
            throw new IllegalArgumentException("headingDegrees must be [0,360): " + headingDegrees);
        if (speedMps != null && speedMps < 0)
            throw new IllegalArgumentException("speedMps must be non-negative: " + speedMps);
        if (vehicleType == null || vehicleType.isBlank())
            throw new IllegalArgumentException("vehicleType required");
        if (availability == null || availability.isBlank())
            throw new IllegalArgumentException("availability required");
    }
}
