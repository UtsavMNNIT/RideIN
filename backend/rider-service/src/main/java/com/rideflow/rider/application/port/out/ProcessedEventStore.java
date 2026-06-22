package com.rideflow.rider.application.port.out;

import java.util.UUID;

/** Output port providing an idempotency ledger for consumed events. */
public interface ProcessedEventStore {

    boolean isProcessed(UUID eventId, String consumerGroup);

    void markProcessed(UUID eventId, String consumerGroup);
}
