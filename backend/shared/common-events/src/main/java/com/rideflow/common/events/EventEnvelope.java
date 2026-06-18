package com.rideflow.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Cross-service event envelope. Every Kafka payload in RideFlow wraps its
 * domain-specific body in this structure.
 *
 * Evolution rules:
 *   - Adding a field        → safe (consumers ignore unknown fields)
 *   - Removing / renaming   → breaking; requires a new major version + new topic
 *   - Changing a field type → breaking
 *
 * `version` tracks the schema version of {@code payload}, not the envelope.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventEnvelope<T>(
        UUID    eventId,
        String  eventType,
        int     version,
        Instant occurredAt,
        String  traceId,
        String  source,
        T       payload
) {

    public static <T> EventEnvelope<T> of(
            String type,
            int    version,
            String source,
            String traceId,
            T      payload) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                type,
                version,
                Instant.now(),
                traceId,
                source,
                payload
        );
    }
}
