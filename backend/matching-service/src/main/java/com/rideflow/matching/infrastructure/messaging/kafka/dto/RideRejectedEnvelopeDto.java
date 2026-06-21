package com.rideflow.matching.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumer-side view of {@code EventEnvelope<RideRejected>}. Mirrors the shared
 * envelope structure but is owned by this service so we can evolve our read
 * model independently.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RideRejectedEnvelopeDto(
        UUID                  eventId,
        String                eventType,
        int                   version,
        Instant               occurredAt,
        String                traceId,
        String                source,
        RideRejectedPayloadDto payload) {
}
