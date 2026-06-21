package com.rideflow.driver.support;

import com.rideflow.driver.application.port.out.TokenIssuer;
import com.rideflow.driver.domain.model.Driver;

import java.time.Instant;

/** Deterministic {@link TokenIssuer} fake — encodes the driver id into the token. */
public class FakeTokenIssuer implements TokenIssuer {

    @Override
    public IssuedToken issue(Driver driver) {
        return new IssuedToken(
                "token-for-" + driver.id(),
                "Bearer",
                3600L,
                Instant.parse("2026-06-20T11:00:00Z"));
    }
}
