package com.rideflow.driver.infrastructure.security;

import com.rideflow.driver.application.port.out.PasswordHasher;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt implementation of the {@link PasswordHasher} port. Uses the default
 * strength (10) which is a sound work-factor for an interactive login path.
 * Only {@code spring-security-crypto} is on the classpath — no servlet filter
 * chain — because authentication is enforced at the gateway.
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
