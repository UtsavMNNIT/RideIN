package com.rideflow.driver.application.port.out;

import java.util.UUID;

/** Output port: idempotency ledger keyed on (eventId, consumerGroup). */
public interface ProcessedEventStore {

    boolean isProcessed(UUID eventId, String consumerGroup);

    void markProcessed(UUID eventId, String consumerGroup);
}
