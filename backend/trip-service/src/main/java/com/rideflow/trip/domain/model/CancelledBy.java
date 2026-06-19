package com.rideflow.trip.domain.model;

/** Who cancelled a trip. {@code SYSTEM} is reserved for automated cancellation. */
public enum CancelledBy {
    RIDER,
    DRIVER,
    SYSTEM
}
