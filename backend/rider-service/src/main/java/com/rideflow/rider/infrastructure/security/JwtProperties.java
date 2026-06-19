package com.rideflow.rider.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Bound from {@code rideflow.security.jwt.*}. The secret must be at least 32
 * bytes (256 bits) for HS256 — validated when the signing key is built.
 */
@ConfigurationProperties(prefix = "rideflow.security.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl) {

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "rideflow-rider-service";
        }
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofHours(12);
        }
    }
}
