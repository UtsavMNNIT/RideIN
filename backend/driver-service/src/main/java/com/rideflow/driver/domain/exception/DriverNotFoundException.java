package com.rideflow.driver.domain.exception;

import java.util.UUID;

/**
 * Raised when a lifecycle operation targets a driver id that does not exist.
 * Maps to HTTP 404 Not Found.
 */
public class DriverNotFoundException extends DriverException {

    public DriverNotFoundException(UUID driverId) {
        super("Driver not found: " + driverId);
    }
}
