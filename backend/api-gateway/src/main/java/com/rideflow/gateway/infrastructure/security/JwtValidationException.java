package com.rideflow.gateway.infrastructure.security;

/** Thrown when a presented JWT is missing, malformed, expired, or untrusted. */
public class JwtValidationException extends RuntimeException {

    public JwtValidationException(String message) {
        super(message);
    }
}
