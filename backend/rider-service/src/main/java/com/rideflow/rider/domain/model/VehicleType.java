package com.rideflow.rider.domain.model;

/**
 * Vehicle class a rider can request. Serialized as its name on
 * {@code rider.ride-requested} events; mirrors the Postgres
 * {@code rider.vehicle_type} enum.
 */
public enum VehicleType {
    STANDARD,
    XL,
    PREMIUM
}
