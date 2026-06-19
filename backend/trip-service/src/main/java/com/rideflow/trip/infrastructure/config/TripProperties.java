package com.rideflow.trip.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for {@code rideflow.trip.*}. The outbox poll/batch/max-attempts
 * and sweeper interval stay on {@code @Value} placeholders in their
 * {@code @Scheduled} beans (annotations need a constant); the offer TTL is bound
 * here for injection into {@code OfferTripService}.
 */
@ConfigurationProperties(prefix = "rideflow.trip")
public record TripProperties(int offerTtlSeconds) {

    public TripProperties {
        if (offerTtlSeconds <= 0) {
            throw new IllegalArgumentException("rideflow.trip.offer-ttl-seconds must be > 0");
        }
    }
}
