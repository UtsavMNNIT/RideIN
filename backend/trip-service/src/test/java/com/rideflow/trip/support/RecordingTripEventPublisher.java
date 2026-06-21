package com.rideflow.trip.support;

import com.rideflow.trip.application.port.out.TripEventPublisher;
import com.rideflow.trip.domain.event.RideAccepted;
import com.rideflow.trip.domain.event.RideCancelled;
import com.rideflow.trip.domain.event.RideCompleted;
import com.rideflow.trip.domain.event.RideRejected;
import com.rideflow.trip.domain.event.RideStarted;

import java.util.ArrayList;
import java.util.List;

/** Records every event published, so tests can assert what was emitted. */
public class RecordingTripEventPublisher implements TripEventPublisher {

    public final List<RideAccepted>  accepted  = new ArrayList<>();
    public final List<RideRejected>  rejected  = new ArrayList<>();
    public final List<RideStarted>   started   = new ArrayList<>();
    public final List<RideCompleted> completed = new ArrayList<>();
    public final List<RideCancelled> cancelled = new ArrayList<>();

    @Override public void publishRideAccepted(RideAccepted event)   { accepted.add(event); }
    @Override public void publishRideRejected(RideRejected event)   { rejected.add(event); }
    @Override public void publishRideStarted(RideStarted event)     { started.add(event); }
    @Override public void publishRideCompleted(RideCompleted event) { completed.add(event); }
    @Override public void publishRideCancelled(RideCancelled event) { cancelled.add(event); }

    public int total() {
        return accepted.size() + rejected.size() + started.size()
                + completed.size() + cancelled.size();
    }
}
