package com.rideflow.matching.infrastructure.messaging.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.common.events.EventTypes;
import com.rideflow.common.events.Topics;
import com.rideflow.matching.application.usecase.DispatchRideUseCase;
import com.rideflow.matching.infrastructure.messaging.kafka.dto.RideRejectedEnvelopeDto;
import com.rideflow.matching.infrastructure.messaging.kafka.dto.RideRejectedPayloadDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link Topics#RIDE_REJECTED} and re-dispatches the ride to the
 * next-best driver, closing the loop that trip-service opens when an offered
 * driver declines or the offer expires.
 *
 * <h3>Contract</h3>
 * Mirrors {@link RideRequestedConsumer}:
 * <ul>
 *   <li><b>Manual ack.</b> The offset commits only after the use case returns;
 *       idempotency (inbound eventId via {@code ProcessedEventStore}) makes a
 *       redelivery safe.</li>
 *   <li><b>Bad payload ⇒ DLQ.</b> Structural/validation failures throw
 *       {@link IllegalArgumentException}, routed straight to the DLQ.</li>
 *   <li><b>Transient failure ⇒ retry.</b> Redis/DB/lock-infra exceptions
 *       propagate unacked and are retried with backoff before DLQ.</li>
 *   <li><b>Unknown event type ⇒ ack &amp; skip</b> (forward-compat).</li>
 * </ul>
 */
@Component
public class RideRejectedConsumer {

    private static final Logger log = LoggerFactory.getLogger(RideRejectedConsumer.class);

    private final ObjectMapper        objectMapper;
    private final DispatchRideUseCase dispatchRide;

    public RideRejectedConsumer(ObjectMapper objectMapper, DispatchRideUseCase dispatchRide) {
        this.objectMapper = objectMapper;
        this.dispatchRide = dispatchRide;
    }

    @KafkaListener(
            topics  = Topics.RIDE_REJECTED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String rawJson, Acknowledgment ack) {
        RideRejectedEnvelopeDto envelope = parse(rawJson);

        if (envelope.eventType() != null
                && !EventTypes.RIDE_REJECTED.equals(envelope.eventType())) {
            log.debug("Ignoring eventType={} (not handled by this consumer)", envelope.eventType());
            ack.acknowledge();
            return;
        }

        RideRejectedPayloadDto p = envelope.payload();
        if (p.rideId() == null || p.driverId() == null) {
            throw new IllegalArgumentException("rideId and driverId are required on ride.rejected");
        }

        dispatchRide.redispatch(envelope.eventId(), p.rideId(), p.driverId());
        ack.acknowledge();
    }

    private RideRejectedEnvelopeDto parse(String rawJson) {
        RideRejectedEnvelopeDto envelope;
        try {
            envelope = objectMapper.readValue(rawJson, RideRejectedEnvelopeDto.class);
        } catch (Exception e) {
            // Non-retryable: malformed JSON / structural mismatch → DLQ.
            throw new IllegalArgumentException("Failed to parse RideRejected envelope", e);
        }
        if (envelope == null || envelope.payload() == null || envelope.eventId() == null) {
            throw new IllegalArgumentException("envelope, eventId, or payload is null");
        }
        return envelope;
    }
}
