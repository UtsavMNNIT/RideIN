package com.rideflow.pricing.application.port.out;

import java.util.UUID;

/** Output port: idempotency ledger tracking which events a consumer group has already processed. */
public interface ProcessedEventStore {

    boolean isProcessed(UUID eventId, String consumerGroup);

    void markProcessed(UUID eventId, String consumerGroup);
}
