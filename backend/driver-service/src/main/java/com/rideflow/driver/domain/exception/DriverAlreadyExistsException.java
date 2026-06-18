package com.rideflow.driver.domain.exception;

/**
 * Raised on registration when a unique attribute (email or phone) is already
 * taken. Maps to HTTP 409 Conflict.
 */
public class DriverAlreadyExistsException extends DriverException {

    public DriverAlreadyExistsException(String attribute, String value) {
        super("Driver already registered with " + attribute + ": " + value);
    }
}
