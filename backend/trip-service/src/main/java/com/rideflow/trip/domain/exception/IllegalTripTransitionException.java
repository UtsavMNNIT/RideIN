package com.rideflow.trip.domain.exception;

import com.rideflow.trip.domain.model.TripStatus;

/** An action was attempted from a state that does not allow it. Maps to HTTP 409. */
public class IllegalTripTransitionException extends TripException {

    public IllegalTripTransitionException(String action, TripStatus from) {
        super("cannot " + action + " a trip in state " + from);
    }
}
