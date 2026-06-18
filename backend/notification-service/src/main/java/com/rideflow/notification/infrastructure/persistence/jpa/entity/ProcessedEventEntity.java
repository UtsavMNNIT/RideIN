package com.rideflow.notification.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "processed_events", schema = "notification")
public class ProcessedEventEntity {

    @EmbeddedId
    private Id id;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEventEntity() {}

    public ProcessedEventEntity(UUID eventId, String consumerGroup, Instant processedAt) {
        this.id          = new Id(eventId, consumerGroup);
        this.processedAt = processedAt;
    }

    public Id      getId()          { return id; }
    public Instant getProcessedAt() { return processedAt; }

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "event_id", nullable = false)
        private UUID eventId;

        @Column(name = "consumer_group", nullable = false, length = 64)
        private String consumerGroup;

        protected Id() {}

        public Id(UUID eventId, String consumerGroup) {
            this.eventId       = eventId;
            this.consumerGroup = consumerGroup;
        }

        public UUID   getEventId()       { return eventId; }
        public String getConsumerGroup() { return consumerGroup; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id other)) return false;
            return Objects.equals(eventId, other.eventId)
                && Objects.equals(consumerGroup, other.consumerGroup);
        }
        @Override public int hashCode() { return Objects.hash(eventId, consumerGroup); }
    }
}
