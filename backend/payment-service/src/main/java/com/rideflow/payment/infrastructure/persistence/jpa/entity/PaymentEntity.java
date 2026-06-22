package com.rideflow.payment.infrastructure.persistence.jpa.entity;

import com.rideflow.payment.domain.model.PaymentStatus;
import com.rideflow.payment.domain.model.Payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA mapping for {@code payment.payments}. {@code created_at}/{@code updated_at}
 * are DB-managed. {@code @Version} gives optimistic locking.
 */
@Entity
@Table(name = "payments", schema = "payment")
public class PaymentEntity {

    @Id
    private UUID id;

    @Column(name = "ride_id", nullable = false, unique = true)
    private UUID rideId;

    @Column(name = "rider_id", nullable = false)
    private UUID riderId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "payment_method_id")
    private UUID paymentMethodId;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected PaymentEntity() {}

    public PaymentEntity(UUID id, UUID rideId, UUID riderId, UUID driverId,
                         BigDecimal amount, String currency, PaymentStatus status,
                         UUID paymentMethodId, String failureReason, Instant settledAt,
                         Instant createdAt) {
        this.id              = id;
        this.rideId          = rideId;
        this.riderId         = riderId;
        this.driverId        = driverId;
        this.amount          = amount;
        this.currency        = currency;
        this.status          = status;
        this.paymentMethodId = paymentMethodId;
        this.failureReason   = failureReason;
        this.settledAt       = settledAt;
        this.createdAt       = createdAt;
    }

    /**
     * Copy the mutable lifecycle fields from a transitioned domain {@link Payment}
     * onto this <em>managed</em> entity so Hibernate dirty-checks the update and
     * bumps {@code @Version}. Identity/amount fields never change.
     */
    public void applyState(Payment p) {
        this.status          = p.status();
        this.driverId        = p.driverId();
        this.paymentMethodId = p.paymentMethodId();
        this.failureReason   = p.failureReason();
        this.settledAt       = p.settledAt();
    }

    public UUID          getId()              { return id; }
    public UUID          getRideId()          { return rideId; }
    public UUID          getRiderId()         { return riderId; }
    public UUID          getDriverId()        { return driverId; }
    public BigDecimal    getAmount()          { return amount; }
    public String        getCurrency()        { return currency; }
    public PaymentStatus getStatus()          { return status; }
    public UUID          getPaymentMethodId() { return paymentMethodId; }
    public String        getFailureReason()   { return failureReason; }
    public Instant       getSettledAt()       { return settledAt; }
    public Instant       getCreatedAt()       { return createdAt; }
    public int           getVersion()         { return version; }
}
