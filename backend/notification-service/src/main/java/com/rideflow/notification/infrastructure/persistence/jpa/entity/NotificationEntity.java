package com.rideflow.notification.infrastructure.persistence.jpa.entity;

import com.rideflow.notification.domain.model.NotificationType;
import com.rideflow.notification.domain.model.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notifications", schema = "notification")
public class NotificationEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private NotificationType type;

    @Column(name = "ride_id")
    private UUID rideId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected NotificationEntity() {}

    public NotificationEntity(UUID id, UUID userId, Role role, NotificationType type,
                              UUID rideId, Map<String, Object> payload, Instant createdAt) {
        this.id        = id;
        this.userId    = userId;
        this.role      = role;
        this.type      = type;
        this.rideId    = rideId;
        this.payload   = payload;
        this.createdAt = createdAt;
    }

    public UUID                getId()           { return id; }
    public UUID                getUserId()       { return userId; }
    public Role                getRole()         { return role; }
    public NotificationType    getType()         { return type; }
    public UUID                getRideId()       { return rideId; }
    public Map<String, Object> getPayload()      { return payload; }
    public Instant             getCreatedAt()    { return createdAt; }
    public Instant             getDeliveredAt()  { return deliveredAt; }
    public Instant             getReadAt()       { return readAt; }

    public void markDelivered(Instant at) { this.deliveredAt = at; }
    public void markRead(Instant at)      { this.readAt      = at; }
}
