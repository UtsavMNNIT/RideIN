package com.rideflow.trip.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Driver-reported actuals at trip completion. */
public record CompleteTripRequest(

        @NotNull @Min(0)
        Integer finalDistanceMeters,

        @NotNull @Min(0)
        Integer finalDurationSeconds
) {}
