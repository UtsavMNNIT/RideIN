package com.rideflow.pricing.infrastructure.messaging.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.common.events.EventTypes;
import com.rideflow.common.events.Topics;
import com.rideflow.pricing.application.usecase.PriceRideUseCase;
import com.rideflow.pricing.application.usecase.PriceRideUseCase.PriceRideCommand;
import com.rideflow.pricing.domain.model.GeoPoint;
import com.rideflow.pricing.domain.model.VehicleType;
import com.rideflow.pricing.infrastructure.messaging.kafka.dto.RideRequestedEnvelopeDto;
import com.rideflow.pricing.infrastructure.messaging.kafka.dto.RideRequestedPayloadDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link Topics#RIDER_RIDE_REQUESTED} and prices the ride.
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Manual ack.</b> The offset commits only after the use case returns;
 *       a crash mid-pricing redelivers, and idempotency (envelope id + the
 *       unique ride_id quote) makes reprocessing safe.</li>
 *   <li><b>Bad payload ⇒ DLQ.</b> Structural/validation failures throw
 *       {@link IllegalArgumentException}, routed to the DLQ on the first try.</li>
 *   <li><b>Transient failure ⇒ retry.</b> DB/broker exceptions propagate unacked;
 *       the error handler backs off and only DLQs after the budget — we never
 *       drop a ride that still needs a fare.</li>
 *   <li><b>Unknown event type ⇒ ack &amp; skip</b> (forward-compat).</li>
 * </ul>
 */
@Component
public class RideRequestedConsumer {

    private static final Logger log = LoggerFactory.getLogger(RideRequestedConsumer.class);

    private final ObjectMapper     objectMapper;
    private final PriceRideUseCase priceRide;

    public RideRequestedConsumer(ObjectMapper objectMapper, PriceRideUseCase priceRide) {
        this.objectMapper = objectMapper;
        this.priceRide    = priceRide;
    }

    @KafkaListener(
            topics  = Topics.RIDER_RIDE_REQUESTED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String rawJson, Acknowledgment ack) {
        RideRequestedEnvelopeDto envelope = parse(rawJson);

        if (envelope.eventType() != null
                && !EventTypes.RIDE_REQUESTED.equals(envelope.eventType())) {
            log.debug("Ignoring eventType={} (not handled by pricing)", envelope.eventType());
            ack.acknowledge();
            return;
        }

        priceRide.price(toCommand(envelope));
        ack.acknowledge();
    }

    private RideRequestedEnvelopeDto parse(String rawJson) {
        RideRequestedEnvelopeDto envelope;
        try {
            envelope = objectMapper.readValue(rawJson, RideRequestedEnvelopeDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse RideRequested envelope", e);
        }
        if (envelope == null || envelope.payload() == null || envelope.eventId() == null) {
            throw new IllegalArgumentException("envelope, eventId, or payload is null");
        }
        return envelope;
    }

    private PriceRideCommand toCommand(RideRequestedEnvelopeDto envelope) {
        RideRequestedPayloadDto p = envelope.payload();
        if (p.rideId() == null || p.riderId() == null || p.requestedAt() == null) {
            throw new IllegalArgumentException("rideId, riderId, and requestedAt are required");
        }
        // Value-object validation (lat/lng bounds, vehicle type) throws
        // IllegalArgumentException → DLQ on a poison message.
        return new PriceRideCommand(
                envelope.eventId(),
                p.rideId(),
                p.riderId(),
                new GeoPoint(p.pickupLat(), p.pickupLng()),
                new GeoPoint(p.dropoffLat(), p.dropoffLng()),
                VehicleType.fromWire(p.vehicleType()),
                p.requestedAt());
    }
}
