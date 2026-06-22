package com.rideflow.pricing.application.port.out;

import com.rideflow.pricing.domain.event.FareQuoted;

/** Output port: publishes the authoritative fare for a ride as a {@code pricing.fare-quoted} event. */
public interface FareEventPublisher {

    void publishFareQuoted(FareQuoted event);
}
