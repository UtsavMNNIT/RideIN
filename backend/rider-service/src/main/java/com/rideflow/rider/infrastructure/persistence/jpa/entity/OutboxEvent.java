package com.rideflow.rider.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional-outbox row ({@code rider.outbox}). Written in the same
 * transaction as the aggregate it describes; drained to Kafka by the relay.
 *
 * <p>{@link #id} equals the {@code EventEnvelope.eventId}, so it doubles as the
 * consumer-side dedupe key for at-least-once delivery.
 */
@Entity
@Table(name = "outbox", schema = "rider")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String topic;

    @Column(name = "partition_key", nullable = false)
    private String partitionKey;

    /** Fully serialized {@code EventEnvelope} JSON; stored in a jsonb column. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {}

    public OutboxEvent(UUID id, String aggregateType, UUID aggregateId, String eventType,
                       String topic, String partitionKey, String payload, Instant createdAt) {
        this.id            = id;
        this.aggregateType = aggregateType;
        this.aggregateId   = aggregateId;
        this.eventType     = eventType;
        this.topic         = topic;
        this.partitionKey  = partitionKey;
        this.payload       = payload;
        this.createdAt     = createdAt;
    }

    public UUID    getId()            { return id; }
    public String  getAggregateType() { return aggregateType; }
    public UUID    getAggregateId()   { return aggregateId; }
    public String  getEventType()     { return eventType; }
    public String  getTopic()         { return topic; }
    public String  getPartitionKey()  { return partitionKey; }
    public String  getPayload()       { return payload; }
    public Instant getCreatedAt()     { return createdAt; }
    public Instant getPublishedAt()   { return publishedAt; }

    public void markPublished(Instant at) { this.publishedAt = at; }
}
