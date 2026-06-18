package com.rideflow.rider.domain.exception;

/**
 * Raised on login when the email is unknown OR the password does not match.
 * Carries a single generic message for both cases so the API does not reveal
 * whether an account exists. Maps to HTTP 401 Unauthorized.
 */
public class InvalidCredentialsException extends RiderException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
