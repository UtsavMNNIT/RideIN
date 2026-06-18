package com.rideflow.rider.domain.exception;

/**
 * Base type for all domain-level rider failures. The {@code api.rest.advice}
 * layer maps each concrete subtype to an HTTP status; the domain stays
 * transport-agnostic.
 */
public abstract class RiderException extends RuntimeException {

    protected RiderException(String message) {
        super(message);
    }
}
