package com.rideflow.driver.domain.event;

/**
 * Outbound port — the application layer asks "publish this event" without
 * knowing or caring about the transport (Kafka, in-process bus, no-op for tests).
 *
 * Adapters live in {@code infrastructure.messaging}. Keeping this interface in
 * the {@code domain} package preserves the dependency-inversion rule: domain
 * defines, infrastructure implements.
 */
public interface DomainEventPublisher {

    /**
     * Publish a driver-location reading. Implementations MUST be non-blocking
     * from the caller's perspective; transient broker failures are absorbed
     * by the adapter (retries + drop-with-metric on exhaustion).
     */
    void publishLocationUpdate(DriverLocationUpdated event);
}
