package com.rideflow.driver.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Location ping. {@code lat}/{@code lng} are required and range-checked;
 * {@code headingDegrees} and {@code speedMps} are optional telemetry;
 * {@code capturedAt} is the device clock — when omitted the controller
 * substitutes server time.
 */
public record UpdateLocationRequest(

        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        Double lat,

        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        Double lng,

        @DecimalMin("0.0") @DecimalMax("359.999")
        Double headingDegrees,

        @DecimalMin("0.0")
        Double speedMps,

        Instant capturedAt
) {}
