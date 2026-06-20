package com.rideflow.driver.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/** Consumer-side view of {@code EventEnvelope<ride lifecycle payload>}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RideLifecycleEnvelopeDto(
        UUID                   eventId,
        String                 eventType,
        int                    version,
        Instant                occurredAt,
        String                 traceId,
        String                 source,
        RideLifecyclePayloadDto payload) {
}
