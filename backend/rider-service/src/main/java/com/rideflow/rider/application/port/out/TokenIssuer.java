package com.rideflow.rider.application.port.out;

import com.rideflow.rider.domain.model.Rider;

import java.time.Instant;

/** Output port for issuing access tokens for authenticated riders. */
public interface TokenIssuer {

    IssuedToken issue(Rider rider);

    /** Immutable access token plus its lifetime metadata. */
    record IssuedToken(String token, String tokenType, long expiresInSeconds, Instant expiresAt) {}
}
