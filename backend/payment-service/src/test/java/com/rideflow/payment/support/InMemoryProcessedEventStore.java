package com.rideflow.payment.support;

import com.rideflow.payment.application.port.out.ProcessedEventStore;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory idempotency store for use-case tests. */
public class InMemoryProcessedEventStore implements ProcessedEventStore {

    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isProcessed(UUID eventId, String consumerGroup) {
        return seen.contains(eventId + "|" + consumerGroup);
    }

    @Override
    public void markProcessed(UUID eventId, String consumerGroup) {
        seen.add(eventId + "|" + consumerGroup);
    }
}
