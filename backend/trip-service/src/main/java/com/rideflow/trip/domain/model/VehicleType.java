package com.rideflow.trip.domain.model;

public enum VehicleType {
    STANDARD,
    XL,
    PREMIUM;

    public static VehicleType fromWire(String raw) {
        if (raw == null) throw new IllegalArgumentException("vehicleType is null");
        try {
            return VehicleType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown vehicleType: " + raw, e);
        }
    }
}
