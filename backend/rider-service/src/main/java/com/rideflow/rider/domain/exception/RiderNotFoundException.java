package com.rideflow.rider.domain.exception;

import java.util.UUID;

/**
 * Raised when an operation targets a rider id that does not exist.
 * Maps to HTTP 404 Not Found.
 */
public class RiderNotFoundException extends RiderException {

    public RiderNotFoundException(UUID riderId) {
        super("Rider not found: " + riderId);
    }
}
