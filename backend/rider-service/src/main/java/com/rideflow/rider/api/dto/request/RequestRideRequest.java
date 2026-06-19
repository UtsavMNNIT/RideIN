package com.rideflow.rider.api.dto.request;

import com.rideflow.rider.domain.model.VehicleType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Ride-request payload: pickup + dropoff coordinates and the desired vehicle
 * class. The rider identity comes from the path (gateway-authorized), not the
 * body.
 */
public record RequestRideRequest(

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
