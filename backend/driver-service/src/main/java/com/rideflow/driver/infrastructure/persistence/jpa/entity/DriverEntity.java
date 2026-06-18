package com.rideflow.driver.infrastructure.persistence.jpa.entity;

import com.rideflow.driver.domain.model.DriverAvailability;
import com.rideflow.driver.domain.model.VehicleType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA mapping for {@code driver.drivers}. Holds the mutable persistence state;
 * the immutable {@link com.rideflow.driver.domain.model.Driver} aggregate is the
 * source of truth for behaviour. Mapping lives in {@code JpaDriverRepository}.
 *
 * <p>Schema is owned by Flyway ({@code V1__init.sql}); Hibernate runs in
 * {@code validate} mode, so the column definitions here must mirror the DDL.
 */
@Entity
@Table(name = "drivers", schema = "driver")
public class DriverEntity {

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

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "vehicle_plate", nullable = false)
    private String vehiclePlate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private DriverAvailability availability;

    @Column(name = "last_lat")
    private Double lastLat;

    @Column(name = "last_lng")
    private Double lastLng;

    @Column(name = "last_location_at")
    private Instant lastLocationAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DriverEntity() {}

    public DriverEntity(UUID id, String email, String phone, String fullName, String passwordHash,
                        VehicleType vehicleType, String vehiclePlate, DriverAvailability availability,
                        Double lastLat, Double lastLng, Instant lastLocationAt,
                        Instant createdAt, Instant updatedAt) {
        this.id             = id;
        this.email          = email;
        this.phone          = phone;
        this.fullName       = fullName;
        this.passwordHash   = passwordHash;
        this.vehicleType    = vehicleType;
        this.vehiclePlate   = vehiclePlate;
        this.availability   = availability;
        this.lastLat        = lastLat;
        this.lastLng        = lastLng;
        this.lastLocationAt = lastLocationAt;
        this.createdAt      = createdAt;
        this.updatedAt      = updatedAt;
    }

    public UUID               getId()             { return id; }
    public String             getEmail()          { return email; }
    public String             getPhone()          { return phone; }
    public String             getFullName()       { return fullName; }
    public String             getPasswordHash()   { return passwordHash; }
    public VehicleType        getVehicleType()    { return vehicleType; }
    public String             getVehiclePlate()   { return vehiclePlate; }
    public DriverAvailability getAvailability()   { return availability; }
    public Double             getLastLat()        { return lastLat; }
    public Double             getLastLng()        { return lastLng; }
    public Instant            getLastLocationAt() { return lastLocationAt; }
    public Instant            getCreatedAt()      { return createdAt; }
    public Instant            getUpdatedAt()      { return updatedAt; }
}
