package com.rideflow.trip.domain.exception;

/** The caller is not the rider/driver bound to this trip. Maps to HTTP 403. */
public class TripActionForbiddenException extends TripException {

    public TripActionForbiddenException(String message) {
        super(message);
    }
}
