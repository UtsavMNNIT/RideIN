package com.rideflow.matching.infrastructure.messaging.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.common.events.EventEnvelope;
import com.rideflow.common.events.EventTypes;
import com.rideflow.common.events.Topics;
import com.rideflow.matching.application.port.out.DispatchEventPublisher;
import com.rideflow.matching.domain.event.RideAssigned;
import com.rideflow.matching.domain.event.RideDispatchFailed;
import com.rideflow.matching.infrastructure.persistence.jpa.entity.OutboxEvent;
import com.rideflow.matching.infrastructure.persistence.jpa.repository.OutboxJpaRepository;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Transactional-outbox implementation of {@link DispatchEventPublisher}.
 *
 * <p>Wraps each domain event in an {@link EventEnvelope}, serializes it, and
 * inserts an {@link OutboxEvent} row — no Kafka contact here. Because this runs
 * inside the dispatch-commit transaction, the event row commits atomically with
 * the ride outcome. {@link OutboxRelay} ships it to the broker afterwards.
 *
 * <p>Partition key is the {@code rideId}, giving per-ride ordering across the
 * assigned/failed lifecycle.
 */
@Component
public class DispatchOutboxPublisher implements DispatchEventPublisher {

    private static final int SCHEMA_VERSION = 1;
    private static final String SOURCE      = "matching-service";

    private final ObjectMapper        objectMapper;
    private final OutboxJpaRepository outbox;

    public DispatchOutboxPublisher(ObjectMapper objectMapper, OutboxJpaRepository outbox) {
        this.objectMapper = objectMapper;
        this.outbox       = outbox;
    }

    @Override
    public void publishRideAssigned(RideAssigned event) {
        enqueue(EventTypes.RIDE_ASSIGNED, Topics.MATCHING_RIDE_ASSIGNED, event.rideId(), event);
    }

    @Override
    public void publishRideDispatchFailed(RideDispatchFailed event) {
        enqueue(EventTypes.RIDE_DISPATCH_FAILED, Topics.MATCHING_DISPATCH_FAILED, event.rideId(), event);
    }

    private <T> void enqueue(String eventType, String topic, UUID aggregateId, T payload) {
        EventEnvelope<T> envelope = EventEnvelope.of(eventType, SCHEMA_VERSION, SOURCE, null, payload);

        String json;
        try {
            json = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            // A non-serializable domain event is a programming error, not runtime.
            throw new IllegalStateException("Failed to serialize " + eventType, e);
        }

        outbox.save(new OutboxEvent(
                envelope.eventId(),
                aggregateId,
                eventType,
                topic,
                aggregateId.toString(),   // partition key → per-ride ordering
                json,
                envelope.occurredAt()));
    }
}
