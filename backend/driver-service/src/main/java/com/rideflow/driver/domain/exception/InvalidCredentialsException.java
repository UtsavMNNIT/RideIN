package com.rideflow.driver.domain.exception;

/**
 * Raised on login when the email is unknown OR the password does not match.
 * Deliberately carries a single generic message for both cases so the API does
 * not reveal whether an account exists (account-enumeration defence).
 * Maps to HTTP 401 Unauthorized.
 */
public class InvalidCredentialsException extends DriverException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
