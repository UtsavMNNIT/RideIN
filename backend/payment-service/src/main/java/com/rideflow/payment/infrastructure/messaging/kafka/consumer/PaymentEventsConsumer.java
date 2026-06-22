package com.rideflow.payment.infrastructure.messaging.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.common.events.Topics;
import com.rideflow.payment.application.usecase.CancelPaymentUseCase;
import com.rideflow.payment.application.usecase.RecordFareUseCase;
import com.rideflow.payment.application.usecase.RecordFareUseCase.RecordFareCommand;
import com.rideflow.payment.application.usecase.SettlePaymentUseCase;
import com.rideflow.payment.application.usecase.SettlePaymentUseCase.SettleCommand;
import com.rideflow.payment.infrastructure.messaging.kafka.dto.EnvelopeDto;
import com.rideflow.payment.infrastructure.messaging.kafka.dto.FareQuotedPayloadDto;
import com.rideflow.payment.infrastructure.messaging.kafka.dto.RideLifecyclePayloadDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Single consumer wired to the three source topics payment-service cares about:
 * {@code pricing.fare-quoted} (open a PENDING payment), {@code ride.completed}
 * (settle), {@code ride.cancelled} (cancel).
 *
 * <p>Manual ack: the offset commits only after the use-case returns, so a crash
 * redelivers and idempotency (envelope id + payments {@code ride_id} unique
 * index) makes it safe. Bad payloads throw {@link IllegalArgumentException} →
 * DLQ on first try.
 */
@Component
public class PaymentEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventsConsumer.class);

    private final ObjectMapper          objectMapper;
    private final RecordFareUseCase     recordFare;
    private final SettlePaymentUseCase  settlePayment;
    private final CancelPaymentUseCase  cancelPayment;

    public PaymentEventsConsumer(ObjectMapper objectMapper,
                                 RecordFareUseCase recordFare,
                                 SettlePaymentUseCase settlePayment,
                                 CancelPaymentUseCase cancelPayment) {
        this.objectMapper  = objectMapper;
        this.recordFare    = recordFare;
        this.settlePayment = settlePayment;
        this.cancelPayment = cancelPayment;
    }

    @KafkaListener(
            topics = {
                    Topics.PRICING_FARE_QUOTED,
                    Topics.RIDE_COMPLETED,
                    Topics.RIDE_CANCELLED
            },
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String rawJson,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          Acknowledgment ack) {

        EnvelopeDto envelope = parseEnvelope(rawJson);
        if (envelope.eventId() == null || envelope.payload() == null) {
            throw new IllegalArgumentException("envelope missing eventId or payload");
        }

        switch (topic) {
            case Topics.PRICING_FARE_QUOTED -> {
                FareQuotedPayloadDto p = readPayload(envelope, FareQuotedPayloadDto.class);
                if (p.rideId() == null || p.riderId() == null || p.total() == null || p.currency() == null) {
                    throw new IllegalArgumentException("FareQuoted missing rideId/riderId/total/currency");
                }
                recordFare.recordFare(new RecordFareCommand(
                        envelope.eventId(), p.rideId(), p.riderId(), p.total(), p.currency()));
            }
            case Topics.RIDE_COMPLETED -> {
                RideLifecyclePayloadDto p = readPayload(envelope, RideLifecyclePayloadDto.class);
                if (p.rideId() == null || p.riderId() == null) {
                    throw new IllegalArgumentException("RideCompleted missing rideId/riderId");
                }
                settlePayment.settle(new SettleCommand(
                        envelope.eventId(), p.rideId(), p.riderId(), p.driverId(),
                        p.fareTotal(), p.currency()));
            }
            case Topics.RIDE_CANCELLED -> {
                RideLifecyclePayloadDto p = readPayload(envelope, RideLifecyclePayloadDto.class);
                if (p.rideId() == null) {
                    throw new IllegalArgumentException("RideCancelled missing rideId");
                }
                cancelPayment.cancel(envelope.eventId(), p.rideId());
            }
            default -> log.warn("Received message on unmapped topic {} — acking and ignoring", topic);
        }

        ack.acknowledge();
    }

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
}
