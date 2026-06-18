package com.rideflow.rider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * rider-service — owns the rider aggregate (registration, authentication) and
 * the ride-request lifecycle entry point. Ride requests are published to Kafka
 * via a transactional outbox; {@code @EnableScheduling} drives the outbox relay.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class RiderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiderServiceApplication.class, args);
    }
}
