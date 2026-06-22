package com.rideflow.rider.application.port.out;

/** Output port for hashing and verifying rider passwords. */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
