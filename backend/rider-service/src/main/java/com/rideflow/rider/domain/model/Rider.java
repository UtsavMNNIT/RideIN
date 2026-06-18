package com.rideflow.rider.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The rider aggregate root — identity and credentials. Immutable; the
 * persistence layer maps it onto a mutable JPA entity. {@code passwordHash} is
 * never exposed through an API DTO.
 */
public record Rider(
        UUID    id,
        String  email,
        String  phone,
        String  fullName,
        String  passwordHash,
        Instant createdAt,
        Instant updatedAt
) {

    public Rider {
        Objects.requireNonNull(id,           "id");
        Objects.requireNonNull(email,        "email");
        Objects.requireNonNull(phone,        "phone");
        Objects.requireNonNull(fullName,     "fullName");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(createdAt,    "createdAt");
        Objects.requireNonNull(updatedAt,    "updatedAt");
    }

    /**
     * Factory for a new rider. Normalizes email (lower-cased) so uniqueness and
     * lookups are case-insensitive. The caller supplies an already-hashed
     * password — the domain never sees the raw secret.
     */
    public static Rider register(String email, String phone, String fullName, String passwordHash) {
        Instant now = Instant.now();
        return new Rider(
                UUID.randomUUID(),
                email.trim().toLowerCase(),
                phone.trim(),
                fullName.trim(),
                passwordHash,
                now,
                now
        );
    }
}
