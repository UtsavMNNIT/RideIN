package com.rideflow.payment.infrastructure.messaging.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.common.events.EventEnvelope;
import com.rideflow.common.events.EventTypes;
import com.rideflow.common.events.Topics;
import com.rideflow.payment.application.port.out.PaymentEventPublisher;
import com.rideflow.payment.domain.event.PaymentSettled;
import com.rideflow.payment.infrastructure.persistence.jpa.entity.OutboxEvent;
import com.rideflow.payment.infrastructure.persistence.jpa.repository.OutboxJpaRepository;

import org.springframework.stereotype.Component;

/**
 * Transactional-outbox implementation of {@link PaymentEventPublisher}. Wraps the
 * event in an {@link EventEnvelope}, serializes it, and inserts an
 * {@link OutboxEvent} row inside the payment transaction — no Kafka contact here.
 * {@code OutboxRelay} ships it afterwards. Partition key = {@code rideId}.
 */
@Component
public class PaymentOutboxPublisher implements PaymentEventPublisher {

    private static final int    SCHEMA_VERSION = 1;
    private static final String SOURCE         = "payment-service";

    private final ObjectMapper        objectMapper;
    private final OutboxJpaRepository outbox;

    public PaymentOutboxPublisher(ObjectMapper objectMapper, OutboxJpaRepository outbox) {
        this.objectMapper = objectMapper;
        this.outbox       = outbox;
    }

    @Override
    public void publishPaymentSettled(PaymentSettled e) {
        EventEnvelope<PaymentSettled> envelope =
                EventEnvelope.of(EventTypes.PAYMENT_SETTLED, SCHEMA_VERSION, SOURCE, null, e);

        String json;
        try {
            json = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize PaymentSettled", ex);
        }

        outbox.save(new OutboxEvent(
                envelope.eventId(),
                e.rideId(),
                EventTypes.PAYMENT_SETTLED,
                Topics.PAYMENT_SETTLED,
                e.rideId().toString(),   // partition key → per-ride ordering
                json,
                envelope.occurredAt()));
    }
}
