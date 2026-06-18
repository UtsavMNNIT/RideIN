package com.rideflow.driver.infrastructure.security;

import com.rideflow.driver.application.port.out.TokenIssuer;
import com.rideflow.driver.domain.model.Driver;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues signed JWT access tokens (HS256) for authenticated drivers.
 *
 * <p>The token is self-describing so the API gateway can authorize downstream
 * requests without a round-trip to this service:
 * <ul>
 *   <li>{@code sub}  — driver id</li>
 *   <li>{@code role} — always {@code DRIVER}</li>
 *   <li>{@code email}, {@code vehicleType} — convenience claims</li>
 * </ul>
 */
@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final SecretKey key;
    private final String    issuer;
    private final Duration  ttl;

    public JwtTokenIssuer(JwtProperties props) {
        // Throws WeakKeyException if the secret is shorter than 256 bits — fail fast at startup.
        this.key    = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.issuer = props.issuer();
        this.ttl    = props.accessTokenTtl();
    }

    @Override
    public IssuedToken issue(Driver driver) {
        Instant now      = Instant.now();
        Instant expiresAt = now.plus(ttl);

        String token = Jwts.builder()
                .issuer(issuer)
                .subject(driver.id().toString())
                .claim("role", "DRIVER")
                .claim("email", driver.email())
                .claim("vehicleType", driver.vehicleType().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();

        return new IssuedToken(token, "Bearer", ttl.toSeconds(), expiresAt);
    }
}
