package com.rideflow.rider.support;

import com.rideflow.rider.application.port.out.TokenIssuer;
import com.rideflow.rider.domain.model.Rider;

import java.time.Instant;

/** Deterministic token issuer for tests. */
public final class FakeTokenIssuer implements TokenIssuer {

    public Rider lastIssuedFor;

    @Override
    public IssuedToken issue(Rider rider) {
        lastIssuedFor = rider;
        return new IssuedToken("token-for-" + rider.id(), "Bearer", 3600L,
                Instant.parse("2030-01-01T00:00:00Z"));
    }
}
