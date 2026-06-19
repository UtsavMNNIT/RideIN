package com.rideflow.rider.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA mapping for {@code rider.riders}. Schema is owned by Flyway; Hibernate
 * runs in {@code validate} mode, so columns here mirror {@code V1__init.sql}.
 */
@Entity
@Table(name = "riders", schema = "rider")
public class RiderEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RiderEntity() {}

    public RiderEntity(UUID id, String email, String phone, String fullName,
                       String passwordHash, Instant createdAt, Instant updatedAt) {
        this.id           = id;
        this.email        = email;
        this.phone        = phone;
        this.fullName     = fullName;
        this.passwordHash = passwordHash;
        this.createdAt    = createdAt;
        this.updatedAt    = updatedAt;
    }

    public UUID    getId()           { return id; }
    public String  getEmail()        { return email; }
    public String  getPhone()        { return phone; }
    public String  getFullName()     { return fullName; }
    public String  getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt()    { return createdAt; }
    public Instant getUpdatedAt()    { return updatedAt; }
}
