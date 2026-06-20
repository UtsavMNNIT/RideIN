package com.rideflow.rider.infrastructure.messaging.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.common.events.Topics;
import com.rideflow.rider.application.usecase.ProjectRideUseCase;
import com.rideflow.rider.infrastructure.messaging.kafka.dto.DispatchFailedPayloadDto;
import com.rideflow.rider.infrastructure.messaging.kafka.dto.EnvelopeDto;
import com.rideflow.rider.infrastructure.messaging.kafka.dto.FareQuotedPayloadDto;
import com.rideflow.rider.infrastructure.messaging.kafka.dto.RideAssignedPayloadDto;
import com.rideflow.rider.infrastructure.messaging.kafka.dto.RideLifecyclePayloadDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Builds the rider's Ride read model by consuming the downstream lifecycle and
 * pricing events. One listener, multiple topics, dispatched by the received
 * topic. Manual ack after the projection commits; bad payloads throw
 * {@link IllegalArgumentException} and route to the DLQ.
 */
@Component
public class RideProjectionConsumer {

    private static final Logger log = LoggerFactory.getLogger(RideProjectionConsumer.class);

    private final ObjectMapper       objectMapper;
    private final ProjectRideUseCase projectRide;

    public RideProjectionConsumer(ObjectMapper objectMapper, ProjectRideUseCase projectRide) {
        this.objectMapper = objectMapper;
        this.projectRide  = projectRide;
    }

    @KafkaListener(
            topics = {
                    Topics.MATCHING_RIDE_ASSIGNED,
                    Topics.MATCHING_DISPATCH_FAILED,
                    Topics.RIDE_STARTED,
                    Topics.RIDE_COMPLETED,
                    Topics.RIDE_CANCELLED,
                    Topics.PRICING_FARE_QUOTED
            },
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String rawJson,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          Acknowledgment ack) {
        EnvelopeDto env = parse(rawJson);
        UUID eventId = env.eventId();

        switch (topic) {
            case Topics.MATCHING_RIDE_ASSIGNED -> {
                RideAssignedPayloadDto p = payload(env, RideAssignedPayloadDto.class);
                projectRide.onAssigned(eventId, require(p.rideId(), "rideId"), p.driverId(), p.matchScore());
            }
            case Topics.MATCHING_DISPATCH_FAILED -> {
                DispatchFailedPayloadDto p = payload(env, DispatchFailedPayloadDto.class);
                projectRide.onDispatchFailed(eventId, require(p.rideId(), "rideId"));
            }
            case Topics.RIDE_STARTED -> {
                RideLifecyclePayloadDto p = payload(env, RideLifecyclePayloadDto.class);
                projectRide.onStarted(eventId, require(p.rideId(), "rideId"));
            }
            case Topics.RIDE_COMPLETED -> {
                RideLifecyclePayloadDto p = payload(env, RideLifecyclePayloadDto.class);
                projectRide.onCompleted(eventId, require(p.rideId(), "rideId"),
                        p.distanceMeters(), p.durationSeconds());
            }
            case Topics.RIDE_CANCELLED -> {
                RideLifecyclePayloadDto p = payload(env, RideLifecyclePayloadDto.class);
                projectRide.onCancelled(eventId, require(p.rideId(), "rideId"));
            }
            case Topics.PRICING_FARE_QUOTED -> {
                FareQuotedPayloadDto p = payload(env, FareQuotedPayloadDto.class);
                projectRide.onFareQuoted(eventId, require(p.rideId(), "rideId"), p.total(), p.currency());
            }
            default -> log.debug("Ignoring unhandled topic {}", topic);
        }

        ack.acknowledge();
    }

    private EnvelopeDto parse(String rawJson) {
        EnvelopeDto env;
        try {
            env = objectMapper.readValue(rawJson, EnvelopeDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse event envelope", e);
        }
        if (env == null || env.eventId() == null || env.payload() == null) {
            throw new IllegalArgumentException("envelope, eventId, or payload is null");
        }
        return env;
    }

    private <T> T payload(EnvelopeDto env, Class<T> type) {
        try {
            return objectMapper.treeToValue(env.payload(), type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read payload as " + type.getSimpleName(), e);
        }
    }

    private static UUID require(UUID value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " is required");
        return value;
    }
}
