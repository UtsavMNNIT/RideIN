package com.rideflow.matching.application.port.out;

import com.rideflow.matching.domain.model.DispatchAttempt;
import com.rideflow.matching.domain.model.Ride;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Output port for persisting and loading the {@link Ride} aggregate plus its dispatch-attempt audit trail. */
public interface RideRepository {

    /** Whether a ride row already exists (used for at-least-once dispatch dedupe). */
    boolean existsById(UUID rideId);

    /** Insert a ride outcome (assigned or failed) together with its dispatch attempts. */
    void saveOutcome(Ride ride, List<DispatchAttempt> attempts);

    /** Update an existing ride outcome (e.g. on redispatch) together with its dispatch attempts. */
    void updateOutcome(Ride ride, List<DispatchAttempt> attempts);

    /** Load a ride by id, if present. */
    Optional<Ride> findById(UUID rideId);
}
