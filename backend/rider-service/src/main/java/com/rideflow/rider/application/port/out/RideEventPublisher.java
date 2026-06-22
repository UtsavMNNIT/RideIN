package com.rideflow.rider.application.port.out;

import com.rideflow.rider.domain.event.RideRequested;

/** Output port for publishing ride domain events (via the transactional outbox). */
public interface RideEventPublisher {

    void publishRideRequested(RideRequested event);
}
