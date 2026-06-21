package com.rideflow.location.infrastructure.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumer-side mirror of {@code driver.availability-changed} payload.
 *
 * <p>Deliberately NOT importing the driver-service class — services share the
 * wire contract, not Java types. Unknown fields are ignored (forward compat);
 * missing fields surface as null and get rejected in the use-case.
 *
 * <p>{@code availability} carries OFFLINE in addition to ONLINE / ON_TRIP — the
 * whole point of this stream is the OFFLINE transition that the location stream
 * never reports. {@code lat}/{@code lng} are the driver's last known position
 * (nullable) so a driver coming ONLINE can be (re)indexed without waiting for a
 * fresh ping.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DriverAvailabilityChangedPayloadDto(
        UUID    driverId,
        String  availability,   // ONLINE | OFFLINE | ON_TRIP
        String  vehicleType,    // STANDARD | XL | PREMIUM
        Double  lat,            // nullable
        Double  lng,            // nullable
        Instant occurredAt
) {}
