package com.rideflow.driver.domain.exception;

/**
 * Base type for all domain-level driver failures. The {@code api.rest.advice}
 * layer maps each concrete subtype to an HTTP status; the domain itself stays
 * transport-agnostic.
 */
public abstract class DriverException extends RuntimeException {

    protected DriverException(String message) {
        super(message);
    }
}
