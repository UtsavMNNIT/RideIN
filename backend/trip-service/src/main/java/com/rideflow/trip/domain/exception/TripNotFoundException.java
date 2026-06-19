package com.rideflow.trip.domain.exception;

import java.util.UUID;

/** No trip exists for a given ride id. */
public class TripNotFoundException extends TripException {

    public TripNotFoundException(UUID rideId) {
        super("trip not found for ride: " + rideId);
    }
}
