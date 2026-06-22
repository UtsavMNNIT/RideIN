package com.rideflow.payment.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** JPA mapping for {@code payment.payment_methods} — mock saved cards. */
@Entity
@Table(name = "payment_methods", schema = "payment")
public class PaymentMethodEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false, length = 4)
    private String last4;

    @Column(nullable = false)
    private String token;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected PaymentMethodEntity() {}

    public PaymentMethodEntity(UUID id, UUID userId, String brand, String last4,
                               String token, boolean isDefault) {
        this.id        = id;
        this.userId    = userId;
        this.brand     = brand;
        this.last4     = last4;
        this.token     = token;
        this.isDefault = isDefault;
    }

    public UUID    getId()        { return id; }
    public UUID    getUserId()    { return userId; }
    public String  getBrand()     { return brand; }
    public String  getLast4()     { return last4; }
    public String  getToken()     { return token; }
    public boolean isDefault()    { return isDefault; }
    public Instant getCreatedAt() { return createdAt; }
}
