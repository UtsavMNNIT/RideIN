package com.rideflow.location.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumer-side mirror of {@code driver.location-updated} payload.
 *
 * <p>Deliberately NOT importing the driver-service class — services share
 * the wire contract, not Java types. This is what keeps the deployment
 * graph loose: we can upgrade driver-service without recompiling consumers.
 *
 * <p>Unknown fields are silently ignored (forward compatibility); missing
 * fields surface as null and get rejected in the use-case.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DriverLocationPayloadDto(
        UUID    driverId,
        Double  lat,
        Double  lng,
        Double  headingDegrees,
        Double  speedMps,
        String  vehicleType,
        String  availability,
        Instant capturedAt
) {}
