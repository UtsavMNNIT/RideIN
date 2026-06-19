package com.rideflow.trip.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/** Consumer-side view of {@code EventEnvelope<RideAssigned>}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RideAssignedEnvelopeDto(
        UUID                   eventId,
        String                 eventType,
        int                    version,
        Instant                occurredAt,
        String                 traceId,
        String                 source,
        RideAssignedPayloadDto payload) {
}
