package com.rideflow.pricing.application.port.out;

import com.rideflow.pricing.domain.model.GeoPoint;
import com.rideflow.pricing.domain.model.TripEstimate;
import com.rideflow.pricing.domain.model.VehicleType;

/** Output port: estimates trip distance/duration between two points for a vehicle type. */
public interface TripEstimator {

    TripEstimate estimate(GeoPoint pickup, GeoPoint dropoff, VehicleType vehicleType);
}
