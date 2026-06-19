package com.rideflow.trip.api.dto.request;

import jakarta.validation.constraints.Size;

/** Optional free-text reason for a cancellation. */
public record CancelTripRequest(

        @Size(max = 128)
        String reason
) {}
