package com.rideflow.location.domain.model;

/**
 * Driver availability as carried on {@code driver.location-updated} events.
 *
 * Note: {@code OFFLINE} is intentionally absent — going offline does not produce
 * a location event. It arrives via {@code driver.availability-changed}, handled
 * by a separate consumer in a later slice.
 */
public enum DriverAvailability {
    ONLINE,
    ON_TRIP;

    public static DriverAvailability fromWire(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("availability is null");
        }
        try {
            return DriverAvailability.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown availability: " + raw, e);
        }
    }
}
