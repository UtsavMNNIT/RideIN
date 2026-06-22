package com.rideflow.driver.application.port.out;

import com.rideflow.driver.domain.model.Driver;

import java.util.Optional;
import java.util.UUID;

/** Output port: persistence and lookup of {@link Driver} aggregates. */
public interface DriverRepository {

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<Driver> findById(UUID id);

    Optional<Driver> findByEmail(String email);

    Driver save(Driver driver);
}
