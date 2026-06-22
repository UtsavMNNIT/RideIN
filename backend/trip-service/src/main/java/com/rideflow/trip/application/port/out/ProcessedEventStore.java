package com.rideflow.trip.application.port.out;

import java.util.UUID;

/** Output port for idempotent inbound-event de-duplication per consumer group. */
public interface ProcessedEventStore {

    boolean isProcessed(UUID eventId, String consumerGroup);

    void markProcessed(UUID eventId, String consumerGroup);
}
