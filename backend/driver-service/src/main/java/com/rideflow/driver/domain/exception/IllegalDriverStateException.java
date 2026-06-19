package com.rideflow.driver.domain.exception;

import java.util.UUID;

/**
 * Raised when a requested transition is illegal for the driver's current state
 * (e.g. updating location while OFFLINE, or going offline while ON_TRIP).
 * Maps to HTTP 409 Conflict.
 */
public class IllegalDriverStateException extends DriverException {

    public IllegalDriverStateException(UUID driverId, String reason) {
        super("Illegal state transition for driver " + driverId + ": " + reason);
    }
}
