package com.rideflow.trip.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA mapping for {@code trip.processed_events} — consumer-side idempotency.
 * Composite key {@code (event_id, consumer_group)}.
 */
@Entity
@Table(name = "processed_events", schema = "trip")
public class ProcessedEventEntity {

    @EmbeddedId
    private Key key;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEventEntity() {}

    public ProcessedEventEntity(UUID eventId, String consumerGroup, Instant processedAt) {
        this.key         = new Key(eventId, consumerGroup);
        this.processedAt = processedAt;
    }

    public Key getKey() { return key; }

    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "event_id", nullable = false)
        private UUID eventId;

        @Column(name = "consumer_group", nullable = false)
        private String consumerGroup;

        protected Key() {}

        public Key(UUID eventId, String consumerGroup) {
            this.eventId       = eventId;
            this.consumerGroup = consumerGroup;
        }

        public UUID   getEventId()       { return eventId; }
        public String getConsumerGroup() { return consumerGroup; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(eventId, k.eventId)
                && Objects.equals(consumerGroup, k.consumerGroup);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventId, consumerGroup);
        }
    }
}
