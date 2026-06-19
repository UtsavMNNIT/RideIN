package com.rideflow.trip.infrastructure.messaging.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.common.events.EventTypes;
import com.rideflow.common.events.Topics;
import com.rideflow.trip.application.usecase.OfferTripService;
import com.rideflow.trip.application.usecase.OfferTripService.OfferCommand;
import com.rideflow.trip.domain.model.GeoPoint;
import com.rideflow.trip.domain.model.VehicleType;
import com.rideflow.trip.infrastructure.messaging.kafka.dto.RideAssignedEnvelopeDto;
import com.rideflow.trip.infrastructure.messaging.kafka.dto.RideAssignedPayloadDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link Topics#MATCHING_RIDE_ASSIGNED} and opens a Trip offer.
 *
 * <p>Manual ack: the offset commits only after {@link OfferTripService} returns,
 * so a crash redelivers and idempotency (envelope id + trips PK) makes it safe.
 * Bad payloads throw {@link IllegalArgumentException} → DLQ on first try;
 * unknown event types are acked and skipped.
 */
@Component
public class RideAssignedConsumer {

    private static final Logger log = LoggerFactory.getLogger(RideAssignedConsumer.class);

    private final ObjectMapper     objectMapper;
    private final OfferTripService offerTrip;

    public RideAssignedConsumer(ObjectMapper objectMapper, OfferTripService offerTrip) {
        this.objectMapper = objectMapper;
        this.offerTrip    = offerTrip;
    }

    @KafkaListener(
            topics  = Topics.MATCHING_RIDE_ASSIGNED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String rawJson, Acknowledgment ack) {
        RideAssignedEnvelopeDto envelope = parse(rawJson);

        if (envelope.eventType() != null
                && !EventTypes.RIDE_ASSIGNED.equals(envelope.eventType())) {
            log.debug("Ignoring eventType={} (not handled by trip-service)", envelope.eventType());
            ack.acknowledge();
            return;
        }

        offerTrip.offer(toCommand(envelope));
        ack.acknowledge();
    }

    private RideAssignedEnvelopeDto parse(String rawJson) {
        RideAssignedEnvelopeDto envelope;
        try {
            envelope = objectMapper.readValue(rawJson, RideAssignedEnvelopeDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse RideAssigned envelope", e);
        }
        if (envelope == null || envelope.payload() == null || envelope.eventId() == null) {
            throw new IllegalArgumentException("envelope, eventId, or payload is null");
        }
        return envelope;
    }

    private OfferCommand toCommand(RideAssignedEnvelopeDto envelope) {
        RideAssignedPayloadDto p = envelope.payload();
        if (p.rideId() == null || p.riderId() == null || p.driverId() == null) {
            throw new IllegalArgumentException("rideId, riderId, and driverId are required");
        }
        return new OfferCommand(
                envelope.eventId(),
                p.rideId(),
                p.riderId(),
                p.driverId(),
                new GeoPoint(p.pickupLat(), p.pickupLng()),
                new GeoPoint(p.dropoffLat(), p.dropoffLng()),
                VehicleType.fromWire(p.vehicleType()),
                p.matchScore());
    }
}
