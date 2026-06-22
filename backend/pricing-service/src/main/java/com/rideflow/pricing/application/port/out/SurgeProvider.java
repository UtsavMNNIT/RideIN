package com.rideflow.pricing.application.port.out;

import com.rideflow.pricing.domain.model.GeoPoint;
import com.rideflow.pricing.domain.model.VehicleType;

import java.math.BigDecimal;

/** Output port: returns the surge multiplier in effect for a vehicle type at a pickup location. */
public interface SurgeProvider {

    BigDecimal multiplierFor(VehicleType vehicleType, GeoPoint pickup);
}
