package com.rideflow.location.infrastructure.messaging.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.common.events.EventTypes;
import com.rideflow.common.events.Topics;
import com.rideflow.location.application.usecase.UpdateDriverLocationUseCase;
import com.rideflow.location.infrastructure.messaging.kafka.dto.LocationEnvelopeDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link Topics#DRIVER_LOCATION_UPDATED}.
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>Manual ack: commit offset only after the use-case returns successfully.</li>
 *   <li>Unknown event types are ignored (forward-compat with multi-event topics
 *       if we ever reuse the topic).</li>
 *   <li>Any thrown exception is caught by the {@code DefaultErrorHandler}:
 *       retriable exceptions back off; non-retriable (validation) go to DLQ
 *       on the first failure.</li>
 * </ul>
 */
@Component
public class DriverLocationConsumer {

    private static final Logger log = LoggerFactory.getLogger(DriverLocationConsumer.class);

    private final ObjectMapper                objectMapper;
    private final UpdateDriverLocationUseCase useCase;

    public DriverLocationConsumer(ObjectMapper objectMapper, UpdateDriverLocationUseCase useCase) {
        this.objectMapper = objectMapper;
        this.useCase      = useCase;
    }

    @KafkaListener(
            topics  = Topics.DRIVER_LOCATION_UPDATED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(String rawJson, Acknowledgment ack) {
        LocationEnvelopeDto envelope;
        try {
            envelope = objectMapper.readValue(rawJson, LocationEnvelopeDto.class);
        } catch (Exception e) {
            // Bad JSON / structural mismatch. Re-throw as IllegalArgumentException
            // so DefaultErrorHandler routes to DLQ (registered as non-retryable).
            throw new IllegalArgumentException("Failed to parse envelope", e);
        }

        if (envelope == null || envelope.payload() == null) {
            throw new IllegalArgumentException("envelope or payload null");
        }

        // Topic could carry future event types; only DRIVER_LOCATION_UPDATED is
        // mapped to this use-case. Unknown types are silently acknowledged so
        // they don't pile up in DLQ.
        if (envelope.eventType() != null
                && !EventTypes.DRIVER_LOCATION_UPDATED.equals(envelope.eventType())) {
            log.debug("Ignoring eventType={} (not handled by this consumer)", envelope.eventType());
            ack.acknowledge();
            return;
        }

        useCase.handle(envelope.payload());
        ack.acknowledge();
    }
}
