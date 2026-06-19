package com.rideflow.pricing.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional-outbox row ({@code pricing.outbox}). Written in the same
 * transaction as the quote; drained to Kafka by {@code OutboxRelay}.
 *
 * <p>{@link #eventId} equals the {@code EventEnvelope.eventId} and doubles as the
 * consumer-side dedupe key for at-least-once delivery.
 */
@Entity
@Table(name = "outbox", schema = "pricing")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // BIGSERIAL
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String topic;

    @Column(name = "partition_key", nullable = false)
    private String partitionKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error")
    private String lastError;

    protected OutboxEvent() {}

    public OutboxEvent(UUID eventId, UUID aggregateId, String eventType, String topic,
                       String partitionKey, String payload, Instant occurredAt) {
        this.eventId      = eventId;
        this.aggregateId  = aggregateId;
        this.eventType    = eventType;
        this.topic        = topic;
        this.partitionKey = partitionKey;
        this.payload      = payload;
        this.occurredAt   = occurredAt;
        this.published    = false;
        this.attemptCount = 0;
    }

    public Long    getId()           { return id; }
    public UUID    getEventId()      { return eventId; }
    public UUID    getAggregateId()  { return aggregateId; }
    public String  getEventType()    { return eventType; }
    public String  getTopic()        { return topic; }
    public String  getPartitionKey() { return partitionKey; }
    public String  getPayload()      { return payload; }
    public Instant getOccurredAt()   { return occurredAt; }
    public boolean isPublished()     { return published; }
    public int     getAttemptCount() { return attemptCount; }

    public void markPublished(Instant at) {
        this.published   = true;
        this.publishedAt = at;
        this.lastError   = null;
    }

    public void recordFailure(String error) {
        this.attemptCount++;
        this.lastError = error;
    }
}
