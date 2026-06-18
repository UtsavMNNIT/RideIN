package com.rideflow.location.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumer-side mirror of {@code EventEnvelope<DriverLocationUpdated>}.
 *
 * Bound only to the JSON shape, not to {@code com.rideflow.common.events.EventEnvelope} —
 * keeps location-service decoupled from producer code paths.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LocationEnvelopeDto(
        UUID                          eventId,
        String                        eventType,
        Integer                       version,
        Instant                       occurredAt,
        String                        traceId,
        String                        source,
        DriverLocationPayloadDto      payload
) {}
