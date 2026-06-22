package com.rideflow.driver.application.port.out;

import com.rideflow.driver.domain.model.Driver;

import java.time.Instant;

/** Output port: issues a signed access token for an authenticated {@link Driver}. */
public interface TokenIssuer {

    IssuedToken issue(Driver driver);

    /** A freshly issued access token plus its lifetime metadata. */
    record IssuedToken(String token, String tokenType, long expiresInSeconds, Instant expiresAt) {}
}
