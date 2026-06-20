package com.rideflow.rider.application.usecase;

import com.rideflow.rider.application.port.out.ProcessedEventStore;
import com.rideflow.rider.application.port.out.RideRepository;
import com.rideflow.rider.domain.model.Ride;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Projects downstream events onto the rider's {@link Ride} read model. Every
 * apply is one transaction, idempotent via {@link ProcessedEventStore}, and the
 * monotonic {@code Ride.markXxx} transitions absorb out-of-order / duplicate
 * delivery (a stale event is a no-op).
 */
@Service
public class ProjectRideUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProjectRideUseCase.class);

    private final RideRepository      rides;
    private final ProcessedEventStore processedEvents;
    private final String              consumerGroup;

    public ProjectRideUseCase(RideRepository rides,
                              ProcessedEventStore processedEvents,
                              @Value("${spring.kafka.consumer.group-id}") String consumerGroup) {
        this.rides           = rides;
        this.processedEvents = processedEvents;
        this.consumerGroup   = consumerGroup;
    }

    @Transactional
    public void onAssigned(UUID eventId, UUID rideId, UUID driverId, Double matchScore) {
        apply(eventId, rideId, ride -> ride.markAssigned(driverId, matchScore));
    }

    @Transactional
    public void onDispatchFailed(UUID eventId, UUID rideId) {
        apply(eventId, rideId, Ride::markNoDrivers);
    }

    @Transactional
    public void onStarted(UUID eventId, UUID rideId) {
        apply(eventId, rideId, Ride::markStarted);
    }

    @Transactional
    public void onCompleted(UUID eventId, UUID rideId, Integer distanceMeters, Integer durationSeconds) {
        apply(eventId, rideId, ride -> ride.markCompleted(distanceMeters, durationSeconds));
    }

    @Transactional
    public void onCancelled(UUID eventId, UUID rideId) {
        apply(eventId, rideId, Ride::markCancelled);
    }

    @Transactional
    public void onFareQuoted(UUID eventId, UUID rideId, BigDecimal total, String currency) {
        apply(eventId, rideId, ride -> ride.withFare(total, currency));
    }

    private void apply(UUID eventId, UUID rideId, UnaryOperator<Ride> mutation) {
        if (processedEvents.isProcessed(eventId, consumerGroup)) {
            return;
        }
        Optional<Ride> found = rides.findById(rideId);
        if (found.isEmpty()) {
            // The ride should always exist (rider created it). A missing row means
            // an event for an unknown ride — record it handled so we don't loop.
            log.warn("Projection event {} references unknown ride {}; marking processed", eventId, rideId);
            processedEvents.markProcessed(eventId, consumerGroup);
            return;
        }
        rides.save(mutation.apply(found.get()));
        processedEvents.markProcessed(eventId, consumerGroup);
    }
}
