package com.rideflow.notification.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A single notification destined for one user.
 *
 * <p>A source Kafka event (e.g. {@code matching.RideAssigned}) typically fans
 * into two {@code Notification} instances — one for the rider, one for the
 * driver. Each carries its own {@link #id()} and its own audience-tailored
 * payload.
 *
 * <p>Immutable: state changes (delivered/read) live on the persistence entity,
 * not on this domain object.
 */
public record Notification(
        UUID                id,
        UUID                userId,
        Role                role,
        NotificationType    type,
        UUID                rideId,          // nullable — always populated today, kept nullable for future event types
        Map<String, Object> payload,
        Instant             createdAt
) {

    public Notification {
        Objects.requireNonNull(id,        "id");
        Objects.requireNonNull(userId,    "userId");
        Objects.requireNonNull(role,      "role");
        Objects.requireNonNull(type,      "type");
        Objects.requireNonNull(payload,   "payload");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /**
     * Convenience constructor — generates a fresh id and {@code createdAt}.
     */
    public static Notification create(UUID userId, Role role, NotificationType type,
                                      UUID rideId, Map<String, Object> payload) {
        return new Notification(
                UUID.randomUUID(),
                userId,
                role,
                type,
                rideId,
                Map.copyOf(payload),
                Instant.now()
        );
    }
}
