package com.rideflow.trip.application.usecase;

import com.rideflow.trip.application.port.out.ProcessedEventStore;
import com.rideflow.trip.application.port.out.TripRepository;
import com.rideflow.trip.domain.model.GeoPoint;
import com.rideflow.trip.domain.model.Trip;
import com.rideflow.trip.domain.model.VehicleType;
import com.rideflow.trip.infrastructure.config.TripProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Consume side of {@code matching.ride-assigned}: open a Trip in {@code OFFERED}
 * state for the assigned driver. No outbound event here — matching already
 * published the assignment.
 *
 * <p>One transaction, double idempotency: the {@link ProcessedEventStore} skips
 * a redelivered envelope, and {@code existsById} + the trips PK make a second
 * offer for the same ride a no-op.
 */
@Service
public class OfferTripService {

    private static final Logger log = LoggerFactory.getLogger(OfferTripService.class);

    private final TripRepository      tripRepository;
    private final ProcessedEventStore processedEvents;
    private final Duration            offerTtl;
    private final String              consumerGroup;

    public OfferTripService(TripRepository tripRepository,
                            ProcessedEventStore processedEvents,
                            TripProperties properties,
                            @Value("${spring.kafka.consumer.group-id}") String consumerGroup) {
        this.tripRepository  = tripRepository;
        this.processedEvents = processedEvents;
        this.offerTtl        = Duration.ofSeconds(properties.offerTtlSeconds());
        this.consumerGroup   = consumerGroup;
    }

    @Transactional
    public void offer(OfferCommand cmd) {
        if (processedEvents.isProcessed(cmd.eventId(), consumerGroup)) {
            log.debug("Skipping already-processed eventId={}", cmd.eventId());
            return;
        }
        if (tripRepository.existsById(cmd.rideId())) {
            log.debug("Trip already open for ride {}; marking event {} processed", cmd.rideId(), cmd.eventId());
            processedEvents.markProcessed(cmd.eventId(), consumerGroup);
            return;
        }

        Instant now = Instant.now();
        Trip trip = Trip.offer(
                cmd.rideId(), cmd.riderId(), cmd.driverId(),
                cmd.pickup(), cmd.dropoff(), cmd.vehicleType(),
                cmd.matchScore(), now, now.plus(offerTtl));

        processedEvents.markProcessed(cmd.eventId(), consumerGroup);
        tripRepository.save(trip);

        log.info("Trip {} OFFERED to driver {} (expires {})", cmd.rideId(), cmd.driverId(), trip.offerExpiresAt());
    }

    /** Inputs derived from a {@code matching.ride-assigned} envelope. */
    public record OfferCommand(
            UUID        eventId,
            UUID        rideId,
            UUID        riderId,
            UUID        driverId,
            GeoPoint    pickup,
            GeoPoint    dropoff,
            VehicleType vehicleType,
            Double      matchScore) {
    }
}
