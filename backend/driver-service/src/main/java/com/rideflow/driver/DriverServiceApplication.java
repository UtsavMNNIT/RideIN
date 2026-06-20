package com.rideflow.driver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * driver-service — owns the driver aggregate: registration, authentication,
 * presence (online/offline/on-trip) and location telemetry.
 *
 * <p>{@code @ConfigurationPropertiesScan} binds {@code @ConfigurationProperties}
 * records (e.g. {@code JwtProperties}) without an explicit registration.
 * {@code @EnableKafka} powers the ride-lifecycle consumer that syncs driver
 * availability (ON_TRIP / back to ONLINE).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableKafka
public class DriverServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriverServiceApplication.class, args);
    }
}
