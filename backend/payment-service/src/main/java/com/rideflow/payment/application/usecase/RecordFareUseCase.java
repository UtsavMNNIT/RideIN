package com.rideflow.payment.application.usecase;

import com.rideflow.payment.application.port.out.PaymentRepository;
import com.rideflow.payment.application.port.out.ProcessedEventStore;
import com.rideflow.payment.domain.model.Money;
import com.rideflow.payment.domain.model.Payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Consume side of {@code pricing.fare-quoted}: open a PENDING payment for the
 * ride, recording the quoted fare so settlement (later, on {@code ride.completed})
 * has an amount.
 *
 * <p>One transaction, double idempotency: the {@link ProcessedEventStore} skips a
 * redelivered envelope, and {@code existsByRideId} + the payments {@code ride_id}
 * unique index make a second open for the same ride a no-op.
 */
@Service
public class RecordFareUseCase {

    private static final Logger log = LoggerFactory.getLogger(RecordFareUseCase.class);

    private final PaymentRepository   payments;
    private final ProcessedEventStore processedEvents;
    private final String              consumerGroup;

    public RecordFareUseCase(PaymentRepository payments,
                             ProcessedEventStore processedEvents,
                             @Value("${spring.kafka.consumer.group-id}") String consumerGroup) {
        this.payments        = payments;
        this.processedEvents = processedEvents;
        this.consumerGroup   = consumerGroup;
    }

    @Transactional
    public void recordFare(RecordFareCommand cmd) {
        if (processedEvents.isProcessed(cmd.eventId(), consumerGroup)) {
            log.debug("Skipping already-processed eventId={}", cmd.eventId());
            return;
        }
        if (payments.existsByRideId(cmd.rideId())) {
            log.debug("Payment already open for ride {}; marking event {} processed", cmd.rideId(), cmd.eventId());
            processedEvents.markProcessed(cmd.eventId(), consumerGroup);
            return;
        }

        Payment payment = Payment.pending(
                cmd.rideId(), cmd.riderId(), Money.of(cmd.total(), cmd.currency()), Instant.now());

        processedEvents.markProcessed(cmd.eventId(), consumerGroup);
        payments.save(payment);

        log.info("Payment {} PENDING for ride {} ({} {})",
                payment.id(), cmd.rideId(), cmd.total(), cmd.currency());
    }

    /** Inputs derived from a {@code pricing.fare-quoted} envelope. */
    public record RecordFareCommand(
            UUID                  eventId,
            UUID                  rideId,
            UUID                  riderId,
            java.math.BigDecimal  total,
            String                currency) {
    }
}
