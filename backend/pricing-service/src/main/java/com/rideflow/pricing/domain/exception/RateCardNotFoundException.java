package com.rideflow.pricing.domain.exception;

import com.rideflow.pricing.domain.model.VehicleType;

/** No rate card configured for a requested vehicle type — pricing cannot proceed. */
public class RateCardNotFoundException extends PricingException {

    public RateCardNotFoundException(VehicleType vehicleType) {
        super("no rate card configured for vehicle type: " + vehicleType);
    }
}
