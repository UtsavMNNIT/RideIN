package com.rideflow.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RideFlow payment-service — owner of fare settlement (simulated).
 *
 * <p>Consumes {@code pricing.fare-quoted} to open a PENDING payment for a ride,
 * then on {@code ride.completed} simulates authorize → capture → settle and
 * publishes {@code payment.settled} via a transactional outbox.
 * {@code ride.cancelled} closes the payment out with no charge.
 * {@code @EnableScheduling} powers the outbox relay.
 *
 * <p>This is a demonstration of the settlement architecture — there is no real
 * payment processor. Settlement is deterministic (always succeeds unless a
 * non-zero {@code rideflow.payment.simulated-failure-rate} is configured).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableKafka
@EnableScheduling
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
