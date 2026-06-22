package com.rideflow.driver.application.port.out;

/** Output port: one-way password hashing and verification. */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
