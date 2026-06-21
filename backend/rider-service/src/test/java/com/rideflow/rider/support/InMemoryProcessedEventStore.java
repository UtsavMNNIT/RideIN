package com.rideflow.rider.support;

import com.rideflow.rider.application.port.out.ProcessedEventStore;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Hand-rolled in-memory idempotency ledger. */
public final class InMemoryProcessedEventStore implements ProcessedEventStore {

    private final Set<String> processed = new HashSet<>();

    @Override
    public boolean isProcessed(UUID eventId, String consumerGroup) {
        return processed.contains(key(eventId, consumerGroup));
    }

    @Override
    public void markProcessed(UUID eventId, String consumerGroup) {
        processed.add(key(eventId, consumerGroup));
    }

    private static String key(UUID eventId, String consumerGroup) {
        return consumerGroup + "|" + eventId;
    }
}
