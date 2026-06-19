package com.rideflow.trip.infrastructure.messaging.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.common.events.EventEnvelope;
import com.rideflow.common.events.EventTypes;
import com.rideflow.common.events.Topics;
import com.rideflow.trip.application.port.out.TripEventPublisher;
import com.rideflow.trip.domain.event.RideAccepted;
import com.rideflow.trip.domain.event.RideCancelled;
import com.rideflow.trip.domain.event.RideCompleted;
import com.rideflow.trip.domain.event.RideRejected;
import com.rideflow.trip.domain.event.RideStarted;
import com.rideflow.trip.infrastructure.persistence.jpa.entity.OutboxEvent;
import com.rideflow.trip.infrastructure.persistence.jpa.repository.OutboxJpaRepository;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Transactional-outbox implementation of {@link TripEventPublisher}. Wraps each
 * lifecycle event in an {@link EventEnvelope}, serializes it, and inserts an
 * {@link OutboxEvent} row inside the trip transaction — no Kafka contact here.
 * {@code OutboxRelay} ships it afterwards. Partition key = {@code rideId} for
 * per-ride ordering across the lifecycle.
 */
@Component
public class TripOutboxPublisher implements TripEventPublisher {

    private static final int    SCHEMA_VERSION = 1;
    private static final String SOURCE         = "trip-service";

    private final ObjectMapper        objectMapper;
    private final OutboxJpaRepository outbox;

    public TripOutboxPublisher(ObjectMapper objectMapper, OutboxJpaRepository outbox) {
        this.objectMapper = objectMapper;
        this.outbox       = outbox;
    }

    @Override
    public void publishRideAccepted(RideAccepted e) {
        enqueue(EventTypes.RIDE_ACCEPTED, Topics.RIDE_ACCEPTED, e.rideId(), e);
    }

    @Override
    public void publishRideRejected(RideRejected e) {
        enqueue(EventTypes.RIDE_REJECTED, Topics.RIDE_REJECTED, e.rideId(), e);
    }

    @Override
    public void publishRideStarted(RideStarted e) {
        enqueue(EventTypes.RIDE_STARTED, Topics.RIDE_STARTED, e.rideId(), e);
    }

    @Override
    public void publishRideCompleted(RideCompleted e) {
        enqueue(EventTypes.RIDE_COMPLETED, Topics.RIDE_COMPLETED, e.rideId(), e);
    }

    @Override
    public void publishRideCancelled(RideCancelled e) {
        enqueue(EventTypes.RIDE_CANCELLED, Topics.RIDE_CANCELLED, e.rideId(), e);
    }

    private <T> void enqueue(String eventType, String topic, UUID rideId, T payload) {
        EventEnvelope<T> envelope = EventEnvelope.of(eventType, SCHEMA_VERSION, SOURCE, null, payload);

        String json;
        try {
            json = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize " + eventType, e);
        }

        outbox.save(new OutboxEvent(
                envelope.eventId(),
                rideId,
                eventType,
                topic,
                rideId.toString(),   // partition key → per-ride ordering
                json,
                envelope.occurredAt()));
    }
}
