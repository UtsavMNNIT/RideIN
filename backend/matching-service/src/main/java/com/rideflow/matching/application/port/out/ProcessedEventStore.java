package com.rideflow.matching.application.port.out;

import java.util.UUID;

/** Output port recording processed inbound event ids per consumer group for at-least-once idempotency. */
public interface ProcessedEventStore {

    /** Whether this event id was already processed by the given consumer group. */
    boolean isProcessed(UUID eventId, String consumerGroup);

    /** Record this event id as processed by the given consumer group. */
    void markProcessed(UUID eventId, String consumerGroup);
}
