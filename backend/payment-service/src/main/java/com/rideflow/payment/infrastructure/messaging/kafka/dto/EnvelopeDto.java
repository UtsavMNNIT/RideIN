package com.rideflow.payment.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Untyped envelope. The inner {@code payload} rides as a {@link JsonNode} so a
 * single consumer can dispatch to the correct typed DTO based on the source
 * topic. Mirrors notification-service's EnvelopeDto.
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
