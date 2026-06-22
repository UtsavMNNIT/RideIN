package com.rideflow.pricing.application.port.out;

import com.rideflow.pricing.domain.model.RateCard;
import com.rideflow.pricing.domain.model.VehicleType;

import java.util.List;

/** Output port: supplies the rate card for a vehicle type and the full set of cards. */
public interface RateCardProvider {

    RateCard cardFor(VehicleType vehicleType);

    List<RateCard> all();
}
