package com.rideflow.driver.domain.event;

import com.rideflow.driver.domain.model.VehicleType;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event payload: a driver's dispatch presence changed
 * (ONLINE / OFFLINE / ON_TRIP).
 *
 * Carried inside {@code EventEnvelope<DriverAvailabilityChanged>} on Kafka topic
 * {@code driver.availability-changed} (key = driverId for per-driver ordering).
 *
 * <p>Consumed by location-service to keep its Redis geo availability index
 * correct so matching never offers rides to offline drivers. The last known
 * location ({@code lat}/{@code lng}) is included — nullable until the driver's
 * first ping — so the consumer can (re)index a driver who comes ONLINE.
 *
 * <p>Cross-service enum fields ({@code availability}, {@code vehicleType}) are
 * serialized as strings to keep consumer coupling loose — receiving services
 * map to their own enums.
 *
 * Schema version 1 — tracked in the envelope. Add new fields freely; never
 * remove or rename without bumping the major version and forking the topic.
 */
public record DriverAvailabilityChanged(
        UUID    driverId,
        String  availability,   // ONLINE | OFFLINE | ON_TRIP
        String  vehicleType,    // STANDARD | XL | PREMIUM
        Double  lat,            // last known latitude, nullable
        Double  lng,            // last known longitude, nullable
        Instant occurredAt
) {

    public DriverAvailabilityChanged {
        if (driverId == null)
            throw new IllegalArgumentException("driverId required");
        if (availability == null || availability.isBlank())
            throw new IllegalArgumentException("availability required");
        if (vehicleType == null || vehicleType.isBlank())
            throw new IllegalArgumentException("vehicleType required");
        if (occurredAt == null)
            throw new IllegalArgumentException("occurredAt required");
        if (lat != null && (lat < -90.0 || lat > 90.0))
            throw new IllegalArgumentException("lat out of range: " + lat);
        if (lng != null && (lng < -180.0 || lng > 180.0))
            throw new IllegalArgumentException("lng out of range: " + lng);
    }

    /** Build from a saved {@link com.rideflow.driver.domain.model.Driver} snapshot. */
    public static DriverAvailabilityChanged from(UUID driverId,
                                                 com.rideflow.driver.domain.model.DriverAvailability availability,
                                                 VehicleType vehicleType,
                                                 com.rideflow.driver.domain.model.GeoPoint lastLocation) {
        return new DriverAvailabilityChanged(
                driverId,
                availability.name(),
                vehicleType.name(),
                lastLocation == null ? null : lastLocation.lat(),
                lastLocation == null ? null : lastLocation.lng(),
                Instant.now()
        );
    }
}
