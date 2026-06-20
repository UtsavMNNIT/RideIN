package com.rideflow.rider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * rider-service — owns the rider aggregate (registration, authentication) and
 * the ride-request lifecycle entry point. Ride requests are published to Kafka
 * via a transactional outbox ({@code @EnableScheduling} drives the relay), and
 * the ride read model is projected from downstream events ({@code @EnableKafka}).
 */
@SpringBootApplication
@EnableKafka
@EnableScheduling
@ConfigurationPropertiesScan
public class RiderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiderServiceApplication.class, args);
    }
}
