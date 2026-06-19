package com.rideflow.pricing.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumer-side view of {@code EventEnvelope<RideRequested>}. Mirrors the shared
 * envelope structure but is owned by this service so the read model can evolve
 * independently.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RideRequestedEnvelopeDto(
        UUID                    eventId,
        String                  eventType,
        int                     version,
        Instant                 occurredAt,
        String                  traceId,
        String                  source,
        RideRequestedPayloadDto payload) {
}
