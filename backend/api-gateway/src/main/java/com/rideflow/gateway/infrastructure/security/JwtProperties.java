package com.rideflow.gateway.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Bound from {@code rideflow.security.jwt.*}.
 *
 * <p><b>Keyset, not a single key.</b> The auth services (rider, driver) currently
 * sign with independent HS256 secrets, so the gateway holds a list and accepts a
 * token if it verifies under <em>any</em> trusted key (and passes the issuer /
 * expiry checks). Converging on one shared secret shrinks the list to one;
 * migrating to RS256/JWKS replaces this with asymmetric public keys — both are
 * config-only changes from here.
 *
 * @param secrets        HMAC signing secrets to trust (each ≥ 32 bytes for HS256)
 * @param allowedIssuers if non-empty, the token's {@code iss} must be in this set
 */
@ConfigurationProperties(prefix = "rideflow.security.jwt")
public record JwtProperties(
        List<String> secrets,
        List<String> allowedIssuers
) {

    public JwtProperties {
        if (secrets == null || secrets.isEmpty()) {
            throw new IllegalArgumentException("rideflow.security.jwt.secrets must contain at least one key");
        }
        allowedIssuers = allowedIssuers == null ? List.of() : List.copyOf(allowedIssuers);
        secrets = List.copyOf(secrets);
    }
}
