package com.rideflow.payment.application.usecase;

import com.rideflow.payment.application.port.out.PaymentEventPublisher;
import com.rideflow.payment.application.port.out.PaymentRepository;
import com.rideflow.payment.application.port.out.ProcessedEventStore;
import com.rideflow.payment.domain.event.PaymentSettled;
import com.rideflow.payment.domain.model.Money;
import com.rideflow.payment.domain.model.Payment;
import com.rideflow.payment.infrastructure.config.PaymentProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Consume side of {@code ride.completed}: settle the ride's fare (simulated) and
 * publish {@code payment.settled} via the transactional outbox.
 *
 * <p>Settlement is a simulated authorize → capture → settle. There is no real
 * payment processor; success is deterministic unless a non-zero
 * {@code rideflow.payment.simulated-failure-rate} is configured (the decision is
 * derived from the rideId hash so it is stable across redeliveries).
 *
 * <p>If no PENDING payment exists yet (fare-quoted not seen — rare; it is
 * published far earlier in the lifecycle), one is opened on the spot using the
 * fare carried on the completion event, or a zero fallback in the default
 * currency, so a receipt always exists.
 */
@Service
public class SettlePaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(SettlePaymentUseCase.class);

    private final PaymentRepository     payments;
    private final PaymentEventPublisher events;
    private final ProcessedEventStore   processedEvents;
    private final PaymentProperties     properties;
    private final String                consumerGroup;

    public SettlePaymentUseCase(PaymentRepository payments,
                                PaymentEventPublisher events,
                                ProcessedEventStore processedEvents,
                                PaymentProperties properties,
                                @Value("${spring.kafka.consumer.group-id}") String consumerGroup) {
        this.payments        = payments;
        this.events          = events;
        this.processedEvents = processedEvents;
        this.properties      = properties;
        this.consumerGroup   = consumerGroup;
    }

    @Transactional
    public void settle(SettleCommand cmd) {
        if (processedEvents.isProcessed(cmd.eventId(), consumerGroup)) {
            log.debug("Skipping already-processed eventId={}", cmd.eventId());
            return;
        }
        processedEvents.markProcessed(cmd.eventId(), consumerGroup);

        Payment payment = payments.findByRideId(cmd.rideId())
                .orElseGet(() -> openFallbackPending(cmd));

        if (payment.status().isTerminal()) {
            log.debug("Payment for ride {} already terminal ({}); nothing to settle",
                    cmd.rideId(), payment.status());
            return;
        }

        Instant now = Instant.now();

        if (shouldFail(cmd.rideId())) {
            Payment failed = payments.save(payment.fail("simulated settlement failure"));
            log.warn("Payment {} FAILED (simulated) for ride {}", failed.id(), cmd.rideId());
            return;
        }

        // Simulated processor handshake.
        Payment settled = payment
                .authorize(payment.paymentMethodId())
                .capture()
                .settle(cmd.driverId(), now);
        settled = payments.save(settled);

        Money fare = settled.fare();
        events.publishPaymentSettled(new PaymentSettled(
                settled.id(), settled.rideId(), settled.riderId(), settled.driverId(),
                fare.amount(), fare.currency(), settled.status().name(), now));

        log.info("Payment {} SETTLED for ride {} ({} {}) -> driver {}",
                settled.id(), cmd.rideId(), fare.amount(), fare.currency(), cmd.driverId());
    }

    private Payment openFallbackPending(SettleCommand cmd) {
        BigDecimal amount   = cmd.fareTotal() != null ? cmd.fareTotal() : BigDecimal.ZERO;
        String     currency = cmd.currency()  != null ? cmd.currency()  : properties.defaultCurrency();
        log.warn("No PENDING payment for ride {} at completion; opening fallback ({} {})",
                cmd.rideId(), amount, currency);
        return Payment.pending(cmd.rideId(), cmd.riderId(), Money.of(amount, currency), Instant.now());
    }

    /** Deterministic per-ride failure decision (no RNG) so redeliveries agree. */
    private boolean shouldFail(UUID rideId) {
        double rate = properties.simulatedFailureRate();
        if (rate <= 0.0) {
            return false;
        }
        int bucket = Math.floorMod(rideId.hashCode(), 100);
        return bucket < (rate * 100);
    }

    /** Inputs derived from a {@code ride.completed} envelope. */
    public record SettleCommand(
            UUID       eventId,
            UUID       rideId,
            UUID       riderId,
            UUID       driverId,
            BigDecimal fareTotal,
            String     currency) {
    }
}
