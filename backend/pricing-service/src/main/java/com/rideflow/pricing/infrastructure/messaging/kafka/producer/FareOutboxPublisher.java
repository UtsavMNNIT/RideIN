package com.rideflow.pricing.infrastructure.messaging.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.common.events.EventEnvelope;
import com.rideflow.common.events.EventTypes;
import com.rideflow.common.events.Topics;
import com.rideflow.pricing.application.port.out.FareEventPublisher;
import com.rideflow.pricing.domain.event.FareQuoted;
import com.rideflow.pricing.infrastructure.persistence.jpa.entity.OutboxEvent;
import com.rideflow.pricing.infrastructure.persistence.jpa.repository.OutboxJpaRepository;

import org.springframework.stereotype.Component;

/**
 * Transactional-outbox implementation of {@link FareEventPublisher}.
 *
 * <p>Wraps {@link FareQuoted} in an {@link EventEnvelope}, serializes it, and
 * inserts an {@link OutboxEvent} row — no Kafka contact here. Because this runs
 * inside the pricing transaction, the event row commits atomically with the
 * quote. {@code OutboxRelay} ships it to the broker afterwards.
 *
 * <p>Partition key is the {@code rideId}, giving per-ride ordering.
 */
@Component
public class FareOutboxPublisher implements FareEventPublisher {

    private static final int    SCHEMA_VERSION = 1;
    private static final String SOURCE         = "pricing-service";

    private final ObjectMapper        objectMapper;
    private final OutboxJpaRepository outbox;

    public FareOutboxPublisher(ObjectMapper objectMapper, OutboxJpaRepository outbox) {
        this.objectMapper = objectMapper;
        this.outbox       = outbox;
    }

    @Override
    public void publishFareQuoted(FareQuoted event) {
        EventEnvelope<FareQuoted> envelope =
                EventEnvelope.of(EventTypes.FARE_QUOTED, SCHEMA_VERSION, SOURCE, null, event);

        String json;
        try {
            json = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize FareQuoted", e);
        }

        outbox.save(new OutboxEvent(
                envelope.eventId(),
                event.rideId(),
                EventTypes.FARE_QUOTED,
                Topics.PRICING_FARE_QUOTED,
                event.rideId().toString(),   // partition key → per-ride ordering
                json,
                envelope.occurredAt()));
    }
}
