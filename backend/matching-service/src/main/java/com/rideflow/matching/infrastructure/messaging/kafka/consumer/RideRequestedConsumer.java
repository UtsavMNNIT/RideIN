package com.rideflow.matching.infrastructure.messaging.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.common.events.EventTypes;
import com.rideflow.common.events.Topics;
import com.rideflow.matching.application.usecase.DispatchRideUseCase;
import com.rideflow.matching.application.usecase.DispatchRideUseCase.DispatchCommand;
import com.rideflow.matching.domain.model.GeoPoint;
import com.rideflow.matching.domain.model.VehicleType;
import com.rideflow.matching.infrastructure.messaging.kafka.dto.RideRequestedEnvelopeDto;
import com.rideflow.matching.infrastructure.messaging.kafka.dto.RideRequestedPayloadDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link Topics#RIDER_RIDE_REQUESTED} and drives dispatch.
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Manual ack.</b> The offset commits only after the use case returns;
 *       a crash mid-dispatch redelivers, and idempotency (envelope id + ride PK)
 *       makes reprocessing safe.</li>
 *   <li><b>Bad payload ⇒ DLQ.</b> Structural/validation failures throw
 *       {@link IllegalArgumentException}, which the error handler routes to the
 *       DLQ on the first try (no pointless retries on a poison message).</li>
 *   <li><b>Transient failure ⇒ retry.</b> Redis/DB/lock-infra exceptions
 *       propagate unacked; the error handler backs off and retries, only DLQ-ing
 *       after the backoff budget — we never drop a dispatchable ride on a blip.</li>
 *   <li><b>Unknown event type ⇒ ack &amp; skip</b> (forward-compat).</li>
 * </ul>
 */
@Component
public class RideRequestedConsumer {

    private static final Logger log = LoggerFactory.getLogger(RideRequestedConsumer.class);

    private final ObjectMapper        objectMapper;
    private final DispatchRideUseCase dispatchRide;

    public RideRequestedConsumer(ObjectMapper objectMapper, DispatchRideUseCase dispatchRide) {
        this.objectMapper = objectMapper;
        this.dispatchRide = dispatchRide;
    }

    @KafkaListener(
            topics  = Topics.RIDER_RIDE_REQUESTED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String rawJson, Acknowledgment ack) {
        RideRequestedEnvelopeDto envelope = parse(rawJson);

        if (envelope.eventType() != null
                && !EventTypes.RIDE_REQUESTED.equals(envelope.eventType())) {
            log.debug("Ignoring eventType={} (not handled by this consumer)", envelope.eventType());
            ack.acknowledge();
            return;
        }

        dispatchRide.dispatch(toCommand(envelope));
        ack.acknowledge();
    }

    private RideRequestedEnvelopeDto parse(String rawJson) {
        RideRequestedEnvelopeDto envelope;
        try {
            envelope = objectMapper.readValue(rawJson, RideRequestedEnvelopeDto.class);
        } catch (Exception e) {
            // Non-retryable: malformed JSON / structural mismatch → DLQ.
            throw new IllegalArgumentException("Failed to parse RideRequested envelope", e);
        }
        if (envelope == null || envelope.payload() == null || envelope.eventId() == null) {
            throw new IllegalArgumentException("envelope, eventId, or payload is null");
        }
        return envelope;
    }

    private DispatchCommand toCommand(RideRequestedEnvelopeDto envelope) {
        RideRequestedPayloadDto p = envelope.payload();
        if (p.rideId() == null || p.riderId() == null || p.requestedAt() == null) {
            throw new IllegalArgumentException("rideId, riderId, and requestedAt are required");
        }
        // Validation lives in the value objects — a bad lat/lng or vehicle type
        // throws IllegalArgumentException here and routes the message to DLQ.
        return new DispatchCommand(
                envelope.eventId(),
                p.rideId(),
                p.riderId(),
                new GeoPoint(p.pickupLat(), p.pickupLng()),
                new GeoPoint(p.dropoffLat(), p.dropoffLng()),
                VehicleType.fromWire(p.vehicleType()),
                p.requestedAt());
    }
}
