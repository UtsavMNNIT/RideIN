package com.rideflow.rider.support;

import com.rideflow.rider.application.port.out.PasswordHasher;

/** Deterministic non-crypto hasher: hash = "hashed:" + raw. */
public final class FakePasswordHasher implements PasswordHasher {

    @Override
    public String hash(String rawPassword) {
        return "hashed:" + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return hash(rawPassword).equals(passwordHash);
    }
}
