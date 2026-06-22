package com.rideflow.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for {@code rideflow.payment.*}. The outbox poll/batch settings
 * stay on {@code @Value} in {@code OutboxRelay} (annotations need constants);
 * the settlement knobs are bound here.
 */
@ConfigurationProperties(prefix = "rideflow.payment")
public record PaymentProperties(String defaultCurrency, double simulatedFailureRate) {

    public PaymentProperties {
        if (defaultCurrency == null || defaultCurrency.isBlank()) {
            defaultCurrency = "INR";
        }
        if (simulatedFailureRate < 0.0 || simulatedFailureRate > 1.0) {
            throw new IllegalArgumentException("rideflow.payment.simulated-failure-rate must be in [0,1]");
        }
    }
}
