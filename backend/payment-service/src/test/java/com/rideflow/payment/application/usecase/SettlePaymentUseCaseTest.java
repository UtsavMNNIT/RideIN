package com.rideflow.payment.application.usecase;

import com.rideflow.payment.application.usecase.SettlePaymentUseCase.SettleCommand;
import com.rideflow.payment.domain.model.Money;
import com.rideflow.payment.domain.model.Payment;
import com.rideflow.payment.domain.model.PaymentStatus;
import com.rideflow.payment.infrastructure.config.PaymentProperties;
import com.rideflow.payment.support.InMemoryPaymentRepository;
import com.rideflow.payment.support.InMemoryProcessedEventStore;
import com.rideflow.payment.support.RecordingPaymentEventPublisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SettlePaymentUseCaseTest {

    private InMemoryPaymentRepository      payments;
    private RecordingPaymentEventPublisher events;
    private InMemoryProcessedEventStore    processed;
    private SettlePaymentUseCase           settle;

    private static final String GROUP = "payment-service";

    @BeforeEach
    void setUp() {
        payments  = new InMemoryPaymentRepository();
        events    = new RecordingPaymentEventPublisher();
        processed = new InMemoryProcessedEventStore();
        settle    = new SettlePaymentUseCase(payments, events, processed,
                new PaymentProperties("INR", 0.0), GROUP);
    }

    @Test
    void settlesAnOpenPendingPaymentAndPublishes() {
        UUID rideId  = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID driver  = UUID.randomUUID();
        payments.save(Payment.pending(rideId, riderId, Money.of(new BigDecimal("142.00"), "INR"), Instant.now()));

        settle.settle(new SettleCommand(UUID.randomUUID(), rideId, riderId, driver, null, null));

        Payment result = payments.findByRideId(rideId).orElseThrow();
        assertThat(result.status()).isEqualTo(PaymentStatus.SETTLED);
        assertThat(result.driverId()).isEqualTo(driver);
        assertThat(events.settled).hasSize(1);
        assertThat(events.settled.get(0).amount()).isEqualByComparingTo("142.00");
        assertThat(events.settled.get(0).currency()).isEqualTo("INR");
    }

    @Test
    void opensFallbackPaymentWhenFareNeverQuoted() {
        UUID rideId  = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID driver  = UUID.randomUUID();

        settle.settle(new SettleCommand(UUID.randomUUID(), rideId, riderId, driver,
                new BigDecimal("99.50"), "INR"));

        Payment result = payments.findByRideId(rideId).orElseThrow();
        assertThat(result.status()).isEqualTo(PaymentStatus.SETTLED);
        assertThat(result.fare().amount()).isEqualByComparingTo("99.50");
        assertThat(events.settled).hasSize(1);
    }

    @Test
    void duplicateEventIsIgnored() {
        UUID rideId  = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        payments.save(Payment.pending(rideId, riderId, Money.of(new BigDecimal("50.00"), "INR"), Instant.now()));

        settle.settle(new SettleCommand(eventId, rideId, riderId, UUID.randomUUID(), null, null));
        settle.settle(new SettleCommand(eventId, rideId, riderId, UUID.randomUUID(), null, null));

        assertThat(events.settled).hasSize(1);
    }

    @Test
    void alwaysFailsWhenFailureRateIsOne() {
        SettlePaymentUseCase failing = new SettlePaymentUseCase(payments, events, processed,
                new PaymentProperties("INR", 1.0), GROUP);
        UUID rideId  = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        payments.save(Payment.pending(rideId, riderId, Money.of(new BigDecimal("10.00"), "INR"), Instant.now()));

        failing.settle(new SettleCommand(UUID.randomUUID(), rideId, riderId, UUID.randomUUID(), null, null));

        assertThat(payments.findByRideId(rideId).orElseThrow().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(events.settled).isEmpty();
    }
}
