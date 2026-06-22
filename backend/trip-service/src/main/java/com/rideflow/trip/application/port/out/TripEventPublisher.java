package com.rideflow.trip.application.port.out;

import com.rideflow.trip.domain.event.RideAccepted;
import com.rideflow.trip.domain.event.RideCancelled;
import com.rideflow.trip.domain.event.RideCompleted;
import com.rideflow.trip.domain.event.RideRejected;
import com.rideflow.trip.domain.event.RideStarted;

/** Output port for emitting trip lifecycle domain events to the outside world. */
public interface TripEventPublisher {

    void publishRideAccepted(RideAccepted event);

    void publishRideRejected(RideRejected event);

    void publishRideStarted(RideStarted event);

    void publishRideCompleted(RideCompleted event);

    void publishRideCancelled(RideCancelled event);
}
