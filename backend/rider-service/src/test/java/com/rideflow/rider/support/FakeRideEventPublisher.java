package com.rideflow.rider.support;

import com.rideflow.rider.application.port.out.RideEventPublisher;
import com.rideflow.rider.domain.event.RideRequested;

import java.util.ArrayList;
import java.util.List;

/** Captures published events for assertions (stands in for the outbox writer). */
public final class FakeRideEventPublisher implements RideEventPublisher {

    public final List<RideRequested> published = new ArrayList<>();

    @Override
    public void publishRideRequested(RideRequested event) {
        published.add(event);
    }
}
