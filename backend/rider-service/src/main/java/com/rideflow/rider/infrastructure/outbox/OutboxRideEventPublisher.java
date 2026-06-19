package com.rideflow.rider.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.common.events.EventEnvelope;
import com.rideflow.common.events.EventTypes;
import com.rideflow.common.events.Topics;
import com.rideflow.rider.application.port.out.RideEventPublisher;
import com.rideflow.rider.domain.event.RideRequested;
import com.rideflow.rider.infrastructure.persistence.jpa.entity.OutboxEvent;
import com.rideflow.rider.infrastructure.persistence.jpa.repository.OutboxJpaRepository;

import org.springframework.stereotype.Component;

/**
 * Transactional-outbox implementation of {@link RideEventPublisher}.
 *
 * <p>Wraps the domain event in an {@link EventEnvelope}, serializes it, and
 * inserts an {@link OutboxEvent} row. No Kafka contact happens here — this runs
 * inside the caller's transaction, so the event row commits atomically with the
 * ride. The {@link OutboxRelay} ships it to the broker afterwards.
 */
@Component
public class OutboxRideEventPublisher implements RideEventPublisher {

    private static final int    SCHEMA_VERSION = 1;
    private static final String SOURCE         = "rider-service";
    private static final String AGGREGATE_TYPE = "RIDE";

    private final ObjectMapper        objectMapper;
    private final OutboxJpaRepository outbox;

    public OutboxRideEventPublisher(ObjectMapper objectMapper, OutboxJpaRepository outbox) {
        this.objectMapper = objectMapper;
        this.outbox       = outbox;
    }

    @Override
    public void publishRideRequested(RideRequested event) {
        EventEnvelope<RideRequested> envelope = EventEnvelope.of(
                EventTypes.RIDE_REQUESTED, SCHEMA_VERSION, SOURCE, /* traceId */ null, event);

        String payload;
        try {
            payload = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            // A non-serializable domain event is a programming error, not a runtime condition.
            throw new IllegalStateException("Failed to serialize RideRequested event", e);
        }

        outbox.save(new OutboxEvent(
                envelope.eventId(),                 // doubles as the dedupe key
                AGGREGATE_TYPE,
                event.rideId(),
                envelope.eventType(),
                Topics.RIDER_RIDE_REQUESTED,
                event.rideId().toString(),          // partition key → per-ride ordering
                payload,
                envelope.occurredAt()));
    }
}
