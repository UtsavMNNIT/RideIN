package com.rideflow.matching.application.port.out;

import com.rideflow.matching.domain.event.RideAssigned;
import com.rideflow.matching.domain.event.RideDispatchFailed;

/** Output port for emitting dispatch lifecycle domain events (assigned / dispatch-failed). */
public interface DispatchEventPublisher {

    /** Publish a successful assignment event. */
    void publishRideAssigned(RideAssigned event);

    /** Publish a dispatch-failure event after the radius ladder is exhausted. */
    void publishRideDispatchFailed(RideDispatchFailed event);
}
