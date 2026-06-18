package com.rideflow.location.domain.model;

/**
 * Vehicle classes used to shard the geo index.
 *
 * Cross-service contract: this enum is mirrored loosely by the {@code vehicleType}
 * string in {@code driver.location-updated} events. New entries are additive —
 * unknown values from upstream are routed to DLQ rather than silently bucketed.
 */
public enum VehicleType {
    STANDARD,
    XL,
    PREMIUM;

    public static VehicleType fromWire(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("vehicleType is null");
        }
        try {
            return VehicleType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown vehicleType: " + raw, e);
        }
    }
}
