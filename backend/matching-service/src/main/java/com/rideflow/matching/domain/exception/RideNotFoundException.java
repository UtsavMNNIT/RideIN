package com.rideflow.matching.domain.exception;

import java.util.UUID;

/** No dispatch record exists for the requested ride id. */
public class RideNotFoundException extends MatchingException {

    public RideNotFoundException(UUID rideId) {
        super("No dispatch record for ride " + rideId);
    }
}
