package com.rideflow.driver.application.usecase;

import com.rideflow.driver.application.port.out.DriverRepository;
import com.rideflow.driver.domain.event.DomainEventPublisher;
import com.rideflow.driver.domain.event.DriverAvailabilityChanged;
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
 *
 * <p>On a successful transition the new presence is broadcast as a
 * {@link DriverAvailabilityChanged} event so location-service can keep its
 * Redis geo availability index correct (drop OFFLINE drivers, re-index ONLINE
 * ones). A failed transition (e.g. going offline while ON_TRIP) throws before
 * any save or publish, so no event is emitted.
 */
@Service
public class DriverAvailabilityUseCase {

    private final DriverRepository    repository;
    private final DomainEventPublisher events;

    public DriverAvailabilityUseCase(DriverRepository repository, DomainEventPublisher events) {
        this.repository = repository;
        this.events     = events;
    }

    @Transactional
    public Driver goOnline(UUID driverId) {
        return saveAndPublish(load(driverId).goOnline());
    }

    @Transactional
    public Driver goOffline(UUID driverId) {
        return saveAndPublish(load(driverId).goOffline());
    }

    private Driver saveAndPublish(Driver transitioned) {
        Driver saved = repository.save(transitioned);
        events.publishAvailabilityChanged(DriverAvailabilityChanged.from(
                saved.id(), saved.availability(), saved.vehicleType(), saved.lastLocation()));
        return saved;
    }

    private Driver load(UUID driverId) {
        return repository.findById(driverId)
                .orElseThrow(() -> new DriverNotFoundException(driverId));
    }
}
