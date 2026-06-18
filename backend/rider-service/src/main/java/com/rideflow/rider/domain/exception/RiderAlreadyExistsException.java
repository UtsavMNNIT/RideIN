package com.rideflow.rider.domain.exception;

/**
 * Raised on registration when a unique attribute (email or phone) is already
 * taken. Maps to HTTP 409 Conflict.
 */
public class RiderAlreadyExistsException extends RiderException {

    public RiderAlreadyExistsException(String attribute, String value) {
        super("Rider already registered with " + attribute + ": " + value);
    }
}
