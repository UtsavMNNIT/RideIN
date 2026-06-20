package com.rideflow.driver.application.usecase;

import com.rideflow.driver.application.port.out.DriverRepository;
import com.rideflow.driver.application.port.out.ProcessedEventStore;
import com.rideflow.driver.domain.model.Driver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Projects ride-lifecycle events onto driver availability:
 * {@code ride.accepted} → ON_TRIP, {@code ride.completed}/{@code ride.cancelled}
 * → back to ONLINE.
 *
 * <p>Each handler is one transaction guarded by the {@link ProcessedEventStore}
 * (keyed on the envelope id), so at-least-once redelivery flips availability at
 * most once. A stray event for an unknown driver is logged and marked processed
 * rather than retried — there is nothing to fix by retrying, and we must not
 * DLQ-loop. The {@code endTrip()} transition is itself tolerant of a driver who
 * is not ON_TRIP.
 */
@Service
public class SyncDriverAvailabilityUseCase {

    private static final Logger log = LoggerFactory.getLogger(SyncDriverAvailabilityUseCase.class);

    private final DriverRepository    repository;
    private final ProcessedEventStore processedEvents;
    private final String              consumerGroup;

    public SyncDriverAvailabilityUseCase(DriverRepository repository,
                                         ProcessedEventStore processedEvents,
                                         @Value("${spring.kafka.consumer.group-id}") String consumerGroup) {
        this.repository      = repository;
        this.processedEvents = processedEvents;
        this.consumerGroup   = consumerGroup;
    }

    /** {@code ride.accepted}: the driver picked up the offered trip. */
    @Transactional
    public void onAccepted(UUID eventId, UUID driverId) {
        apply(eventId, driverId, Driver::goOnTrip, "ACCEPTED");
    }

    /** {@code ride.completed} / {@code ride.cancelled}: the trip ended. */
    @Transactional
    public void onTripEnded(UUID eventId, UUID driverId) {
        apply(eventId, driverId, Driver::endTrip, "ENDED");
    }

    private void apply(UUID eventId, UUID driverId, java.util.function.UnaryOperator<Driver> transition, String label) {
        if (processedEvents.isProcessed(eventId, consumerGroup)) {
            log.debug("Skipping already-processed eventId={}", eventId);
            return;
        }
        Optional<Driver> driver = repository.findById(driverId);
        if (driver.isEmpty()) {
            // Stray/unknown driver — nothing to retry. Record and move on.
            log.warn("Driver {} not found for {} event {}; marking processed", driverId, label, eventId);
            processedEvents.markProcessed(eventId, consumerGroup);
            return;
        }
        repository.save(transition.apply(driver.get()));
        processedEvents.markProcessed(eventId, consumerGroup);
        log.info("Driver {} availability synced ({})", driverId, label);
    }
}
