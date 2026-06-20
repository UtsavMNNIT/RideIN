package com.rideflow.rider.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Generic consumer-side view of an {@code EventEnvelope}. The payload is kept as
 * a {@link JsonNode} and converted to the right per-topic DTO once the topic is
 * known (the projection consumer handles several event shapes).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EnvelopeDto(
        UUID     eventId,
        String   eventType,
        int      version,
        Instant  occurredAt,
        String   traceId,
        String   source,
        JsonNode payload) {
}
