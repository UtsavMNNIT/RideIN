package com.rideflow.trip.application.port.out;

import com.rideflow.trip.domain.model.Trip;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Output port for persisting and querying {@link Trip} aggregates. */
public interface TripRepository {

    Trip save(Trip trip);

    Optional<Trip> findById(UUID rideId);

    boolean existsById(UUID rideId);

    List<UUID> findExpiredOfferIds(Instant cutoff, int limit);
}
