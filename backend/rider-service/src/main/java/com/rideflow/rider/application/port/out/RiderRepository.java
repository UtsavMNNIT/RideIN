package com.rideflow.rider.application.port.out;

import com.rideflow.rider.domain.model.Rider;

import java.util.Optional;
import java.util.UUID;

/** Output port for persisting and querying {@link Rider} aggregates. */
public interface RiderRepository {

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsById(UUID id);

    Optional<Rider> findById(UUID id);

    Optional<Rider> findByEmail(String email);

    Rider save(Rider rider);
}
