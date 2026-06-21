package com.rideflow.driver.support;

import com.rideflow.driver.application.port.out.PasswordHasher;

/**
 * Deterministic {@link PasswordHasher} fake. "Hashing" is a reversible prefix so
 * tests stay readable: {@code matches} simply re-hashes and compares.
 */
public class FakePasswordHasher implements PasswordHasher {

    static final String PREFIX = "hashed:";

    @Override
    public String hash(String rawPassword) {
        return PREFIX + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return hash(rawPassword).equals(passwordHash);
    }
}
