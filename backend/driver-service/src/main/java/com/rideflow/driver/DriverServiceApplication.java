package com.rideflow.driver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * driver-service — owns the driver aggregate: registration, authentication,
 * presence (online/offline) and location telemetry.
 *
 * <p>{@code @ConfigurationPropertiesScan} binds {@code @ConfigurationProperties}
 * records (e.g. {@code JwtProperties}) without an explicit registration.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DriverServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriverServiceApplication.class, args);
    }
}
