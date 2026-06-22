package com.rideflow.payment.application.usecase;

import com.rideflow.payment.application.port.out.PaymentRepository;
import com.rideflow.payment.application.port.out.ProcessedEventStore;
import com.rideflow.payment.domain.model.Payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Consume side of {@code ride.cancelled}: close the payment out with no charge.
 * If no payment exists (fare never quoted) the event is simply acked.
 */
@Service
public class CancelPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(CancelPaymentUseCase.class);

    private final PaymentRepository   payments;
    private final ProcessedEventStore processedEvents;
    private final String              consumerGroup;

    public CancelPaymentUseCase(PaymentRepository payments,
                                ProcessedEventStore processedEvents,
                                @Value("${spring.kafka.consumer.group-id}") String consumerGroup) {
        this.payments        = payments;
        this.processedEvents = processedEvents;
        this.consumerGroup   = consumerGroup;
    }

    @Transactional
    public void cancel(UUID eventId, UUID rideId) {
        if (processedEvents.isProcessed(eventId, consumerGroup)) {
            log.debug("Skipping already-processed eventId={}", eventId);
            return;
        }
        processedEvents.markProcessed(eventId, consumerGroup);

        payments.findByRideId(rideId).ifPresent(payment -> {
            if (payment.status().isTerminal()) {
                log.debug("Payment for ride {} already terminal ({}); no cancel", rideId, payment.status());
                return;
            }
            Payment cancelled = payments.save(payment.cancel("ride cancelled"));
            log.info("Payment {} CANCELLED (ride {} cancelled)", cancelled.id(), rideId);
        });
    }
}
