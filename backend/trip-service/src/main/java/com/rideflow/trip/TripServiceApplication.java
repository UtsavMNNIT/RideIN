package com.rideflow.trip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RideFlow trip-service — owner of the post-assignment ride lifecycle.
 *
 * <p>Consumes {@code matching.ride-assigned} to open a Trip offer, drives the
 * driver/rider action state machine (accept → arrive → start → complete, plus
 * reject/expire/cancel), and publishes {@code ride.accepted/rejected/started/
 * completed/cancelled} via a transactional outbox. {@code @EnableScheduling}
 * powers both the outbox relay and the offer-expiry sweeper.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableKafka
@EnableScheduling
public class TripServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TripServiceApplication.class, args);
    }
}
