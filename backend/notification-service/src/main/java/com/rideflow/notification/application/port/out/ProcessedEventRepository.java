package com.rideflow.notification.application.port.out;

import java.util.UUID;

/** Output port acting as the idempotency ledger for consumed source events. */
public interface ProcessedEventRepository {

    boolean markProcessed(UUID eventId, String consumerGroup);
}
