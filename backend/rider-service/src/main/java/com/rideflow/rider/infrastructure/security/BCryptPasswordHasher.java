package com.rideflow.rider.infrastructure.security;

import com.rideflow.rider.application.port.out.PasswordHasher;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt implementation of {@link PasswordHasher} (default strength 10). Only
 * {@code spring-security-crypto} is on the classpath — no servlet filter chain —
 * since authentication is enforced at the gateway.
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }
}
