package com.rideflow.pricing.application.port.out;

import com.rideflow.pricing.domain.model.GeoPoint;
import com.rideflow.pricing.domain.model.VehicleType;

/** Output port: counts available drivers of a vehicle type near a pickup within a radius. */
public interface DriverSupplyProvider {

    int availableDriversNear(VehicleType vehicleType, GeoPoint pickup, int radiusMeters);
}
