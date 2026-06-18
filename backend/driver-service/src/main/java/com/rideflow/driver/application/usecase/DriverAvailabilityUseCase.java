package com.rideflow.driver.application.usecase;

import com.rideflow.driver.application.port.out.DriverRepository;
import com.rideflow.driver.domain.exception.DriverNotFoundException;
import com.rideflow.driver.domain.model.Driver;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Drives the driver's presence transitions (online / offline).
 *
 * <p>Both operations are a load-mutate-save unit of work, so each runs in a
 * single transaction to prevent a lost update under concurrent presence
 * toggles. The transition rules themselves live on the {@link Driver}
 * aggregate; this service only orchestrates persistence.
 */
@Service
public class DriverAvailabilityUseCase {

    private final DriverRepository repository;

    public DriverAvailabilityUseCase(DriverRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Driver goOnline(UUID driverId) {
        return repository.save(load(driverId).goOnline());
    }

    @Transactional
    public Driver goOffline(UUID driverId) {
        return repository.save(load(driverId).goOffline());
    }

    private Driver load(UUID driverId) {
        return repository.findById(driverId)
                .orElseThrow(() -> new DriverNotFoundException(driverId));
    }
}
