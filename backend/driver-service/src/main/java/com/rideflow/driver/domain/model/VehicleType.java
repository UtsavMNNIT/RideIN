package com.rideflow.driver.domain.model;

/**
 * Vehicle class a driver operates. Serialized as its name across service
 * boundaries (see {@code DriverLocationUpdated}); kept in lock-step with the
 * Postgres {@code driver.vehicle_type} enum.
 */
public enum VehicleType {
    STANDARD,
    XL,
    PREMIUM
}
