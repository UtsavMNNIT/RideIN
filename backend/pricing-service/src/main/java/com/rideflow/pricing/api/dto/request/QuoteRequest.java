package com.rideflow.pricing.api.dto.request;

import com.rideflow.pricing.domain.model.VehicleType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Up-front quote request: pickup + dropoff coordinates and the desired vehicle
 * class. {@code riderId} is optional — anonymous "how much?" lookups are allowed
 * before a rider commits.
 */
public record QuoteRequest(

        UUID riderId,

        @NotNull @DecimalMin("-90.0")  @DecimalMax("90.0")
        Double pickupLat,

        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        Double pickupLng,

        @NotNull @DecimalMin("-90.0")  @DecimalMax("90.0")
        Double dropoffLat,

        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        Double dropoffLng,

        @NotNull
        VehicleType vehicleType
) {}
