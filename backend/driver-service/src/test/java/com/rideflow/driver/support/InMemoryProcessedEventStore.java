package com.rideflow.driver.support;

import com.rideflow.driver.application.port.out.ProcessedEventStore;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** In-memory idempotency ledger keyed on (eventId, consumerGroup). */
public class InMemoryProcessedEventStore implements ProcessedEventStore {

    private final Set<String> processed = new HashSet<>();

    private static String key(UUID eventId, String consumerGroup) {
        return eventId + "::" + consumerGroup;
    }

    @Override
    public boolean isProcessed(UUID eventId, String consumerGroup) {
        return processed.contains(key(eventId, consumerGroup));
    }

    @Override
    public void markProcessed(UUID eventId, String consumerGroup) {
        processed.add(key(eventId, consumerGroup));
    }

    public int size() {
        return processed.size();
    }
}
