package com.rideflow.rider.infrastructure.security;

import com.rideflow.rider.application.port.out.TokenIssuer;
import com.rideflow.rider.domain.model.Rider;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues signed JWT access tokens (HS256) for authenticated riders. The token
 * is self-describing so the gateway can authorize downstream requests:
 * {@code sub} = rider id, {@code role} = RIDER, plus an {@code email} claim.
 */
@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final SecretKey key;
    private final String    issuer;
    private final Duration  ttl;

    public JwtTokenIssuer(JwtProperties props) {
        // Throws WeakKeyException if the secret is < 256 bits — fail fast at startup.
        this.key    = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.issuer = props.issuer();
        this.ttl    = props.accessTokenTtl();
    }

    @Override
    public IssuedToken issue(Rider rider) {
        Instant now       = Instant.now();
        Instant expiresAt = now.plus(ttl);

        String token = Jwts.builder()
                .issuer(issuer)
                .subject(rider.id().toString())
                .claim("role", "RIDER")
                .claim("email", rider.email())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();

        return new IssuedToken(token, "Bearer", ttl.toSeconds(), expiresAt);
    }
}
