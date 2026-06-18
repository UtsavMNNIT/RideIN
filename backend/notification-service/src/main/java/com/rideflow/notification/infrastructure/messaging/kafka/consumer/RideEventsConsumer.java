package com.rideflow.notification.infrastructure.messaging.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.common.events.Topics;
import com.rideflow.notification.application.usecase.PublishNotificationUseCase;
import com.rideflow.notification.domain.model.Notification;
import com.rideflow.notification.infrastructure.messaging.kafka.serde.EnvelopeDto;
import com.rideflow.notification.infrastructure.messaging.kafka.serde.RideAssignedPayloadDto;
import com.rideflow.notification.infrastructure.messaging.kafka.serde.RideLifecyclePayloadDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Single consumer wired to all three source topics — a notification service
 * has the same flow for every event (idempotency → factory → persist →
 * broadcast), so one listener with topic-based dispatch is simpler than three
 * near-identical classes.
 *
 * <p>Failure semantics inherited from the container:
 * <ul>
 *   <li>Deserialization errors → not retried → DLQ on the first failure
 *       (registered as non-retryable in {@link com.rideflow.notification.infrastructure.messaging.kafka.topology.KafkaConsumerConfig}).</li>
 *   <li>Transient business failures → retried with exponential backoff up to
 *       3s of total wait, then DLQ.</li>
 *   <li>Duplicate deliveries → silently acked via {@code processed_events}.</li>
 * </ul>
 */
@Component
public class RideEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(RideEventsConsumer.class);

    private final ObjectMapper                  objectMapper;
    private final NotificationFactory           factory;
    private final PublishNotificationUseCase    useCase;
    private final String                        consumerGroup;

    public RideEventsConsumer(ObjectMapper objectMapper,
                              NotificationFactory factory,
                              PublishNotificationUseCase useCase,
                              @Value("${spring.kafka.consumer.group-id}") String consumerGroup) {
        this.objectMapper  = objectMapper;
        this.factory       = factory;
        this.useCase       = useCase;
        this.consumerGroup = consumerGroup;
    }

    @KafkaListener(
            topics = {
                    Topics.MATCHING_RIDE_ASSIGNED,
                    Topics.RIDE_STARTED,
                    Topics.RIDE_COMPLETED
            },
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(String rawJson,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          Acknowledgment ack) {

        EnvelopeDto envelope = parseEnvelope(rawJson);
        if (envelope.eventId() == null || envelope.payload() == null) {
            throw new IllegalArgumentException("envelope missing eventId or payload");
        }

        List<Notification> notifications = switch (topic) {
            case Topics.MATCHING_RIDE_ASSIGNED -> {
                RideAssignedPayloadDto p = readPayload(envelope, RideAssignedPayloadDto.class);
                validateAssigned(p);
                yield factory.fromRideAssigned(p);
            }
            case Topics.RIDE_STARTED -> {
                RideLifecyclePayloadDto p = readPayload(envelope, RideLifecyclePayloadDto.class);
                validateLifecycle(p);
                yield factory.fromRideStarted(p);
            }
            case Topics.RIDE_COMPLETED -> {
                RideLifecyclePayloadDto p = readPayload(envelope, RideLifecyclePayloadDto.class);
                validateLifecycle(p);
                yield factory.fromRideCompleted(p);
            }
            default -> {
                log.warn("Received message on unmapped topic {} — acking and ignoring", topic);
                yield List.of();
            }
        };

        if (notifications.isEmpty()) {
            ack.acknowledge();
            return;
        }

        useCase.handle(envelope.eventId(), consumerGroup, notifications);
        ack.acknowledge();
    }

    // -----------------------------------------------------------------------
    // Parsing — IllegalArgumentException routes straight to DLQ
    // -----------------------------------------------------------------------

    private EnvelopeDto parseEnvelope(String json) {
        try {
            return objectMapper.readValue(json, EnvelopeDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse envelope", e);
        }
    }

    private <T> T readPayload(EnvelopeDto envelope, Class<T> type) {
        try {
            return objectMapper.treeToValue(envelope.payload(), type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse payload as " + type.getSimpleName(), e);
        }
    }

    private void validateAssigned(RideAssignedPayloadDto p) {
        if (p.rideId() == null || p.riderId() == null || p.driverId() == null) {
            throw new IllegalArgumentException("RideAssigned missing rideId/riderId/driverId");
        }
    }

    private void validateLifecycle(RideLifecyclePayloadDto p) {
        if (p.rideId() == null || p.riderId() == null || p.driverId() == null) {
            throw new IllegalArgumentException("Ride lifecycle event missing rideId/riderId/driverId");
        }
    }
}
