package com.rideflow.rider.application.port.out;

import com.rideflow.rider.domain.model.Ride;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Output port for persisting and querying {@link Ride} read-model aggregates. */
public interface RideRepository {

    Ride save(Ride ride);

    Optional<Ride> findById(UUID rideId);

    List<Ride> findByRider(UUID riderId, int page, int size);

    /**
     * Completed rides assigned to {@code driverId}, optionally bounded by a
     * {@code [from, to)} request-time window (null bounds are open). Used to
     * compute driver earnings.
     */
    List<Ride> findCompletedForDriver(UUID driverId, Instant from, Instant to);
}
