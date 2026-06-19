package com.rideflow.trip.domain.exception;

/** Base type for all trip-domain failures. */
public abstract class TripException extends RuntimeException {

    protected TripException(String message) {
        super(message);
    }
}
