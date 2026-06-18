package com.rideflow.notification.infrastructure.messaging.kafka.serde;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Untyped envelope. We let the inner {@code payload} ride as a {@link JsonNode}
 * so a single consumer-side parser can dispatch to the correct typed DTO
 * based on {@code eventType}. Saves declaring a generic-typed envelope per
 * payload class.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EnvelopeDto(
        UUID     eventId,
        String   eventType,
        Integer  version,
        Instant  occurredAt,
        String   traceId,
        String   source,
        JsonNode payload
) {}
