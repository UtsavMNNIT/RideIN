package com.rideflow.driver.support;

import com.rideflow.driver.domain.event.DomainEventPublisher;
import com.rideflow.driver.domain.event.DriverAvailabilityChanged;
import com.rideflow.driver.domain.event.DriverLocationUpdated;

import java.util.ArrayList;
import java.util.List;

/** Records published domain events so tests can assert on them. */
public class RecordingDomainEventPublisher implements DomainEventPublisher {

    public final List<DriverLocationUpdated>     locationUpdates  = new ArrayList<>();
    public final List<DriverAvailabilityChanged> availabilityChanges = new ArrayList<>();

    @Override
    public void publishLocationUpdate(DriverLocationUpdated event) {
        locationUpdates.add(event);
    }

    @Override
    public void publishAvailabilityChanged(DriverAvailabilityChanged event) {
        availabilityChanges.add(event);
    }
}
