package com.rideflow.driver.infrastructure.messaging.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.common.events.Topics;
import com.rideflow.driver.application.usecase.SyncDriverAvailabilityUseCase;
import com.rideflow.driver.infrastructure.messaging.kafka.dto.RideLifecycleEnvelopeDto;
import com.rideflow.driver.infrastructure.messaging.kafka.dto.RideLifecyclePayloadDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumes ride-lifecycle events and keeps driver availability in sync:
 * {@code ride.accepted} → ON_TRIP, {@code ride.completed}/{@code ride.cancelled}
 * → ONLINE.
 *
 * <p>Manual ack after the use case returns (a crash redelivers; idempotency
 * makes it safe). Bad payloads → {@link IllegalArgumentException} → DLQ on first
 * try. Unknown topics are acked and skipped.
 */
@Component
public class RideLifecycleConsumer {

    private static final Logger log = LoggerFactory.getLogger(RideLifecycleConsumer.class);

    private final ObjectMapper                  objectMapper;
    private final SyncDriverAvailabilityUseCase sync;

    public RideLifecycleConsumer(ObjectMapper objectMapper, SyncDriverAvailabilityUseCase sync) {
        this.objectMapper = objectMapper;
        this.sync         = sync;
    }

    @KafkaListener(
            topics = { Topics.RIDE_ACCEPTED, Topics.RIDE_COMPLETED, Topics.RIDE_CANCELLED },
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String rawJson,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          Acknowledgment ack) {
        RideLifecycleEnvelopeDto envelope = parse(rawJson);
        RideLifecyclePayloadDto p = envelope.payload();

        switch (topic) {
            case Topics.RIDE_ACCEPTED  -> sync.onAccepted(envelope.eventId(), p.driverId());
            case Topics.RIDE_COMPLETED,
                 Topics.RIDE_CANCELLED -> sync.onTripEnded(envelope.eventId(), p.driverId());
            default -> log.debug("Ignoring unhandled topic {}", topic);
        }
        ack.acknowledge();
    }

    private RideLifecycleEnvelopeDto parse(String rawJson) {
        RideLifecycleEnvelopeDto envelope;
        try {
            envelope = objectMapper.readValue(rawJson, RideLifecycleEnvelopeDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse ride-lifecycle envelope", e);
        }
        if (envelope == null || envelope.eventId() == null
                || envelope.payload() == null || envelope.payload().driverId() == null) {
            throw new IllegalArgumentException("envelope, eventId, payload, or driverId is null");
        }
        return envelope;
    }
}
