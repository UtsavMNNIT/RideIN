package com.rideflow.matching.infrastructure.persistence.jpa.entity;

import com.rideflow.matching.domain.model.RideStatus;
import com.rideflow.matching.domain.model.VehicleType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA mapping for {@code matching.rides}. {@code created_at}/{@code updated_at}
 * are DB-managed (default + trigger); we never write them from Java.
 */
@Entity
@Table(name = "rides", schema = "matching")
public class RideEntity {

    @Id
    private UUID id;

    @Column(name = "rider_id", nullable = false)
    private UUID riderId;

    @Column(name = "pickup_lat", nullable = false)
    private double pickupLat;

    @Column(name = "pickup_lng", nullable = false)
    private double pickupLng;

    @Column(name = "dropoff_lat", nullable = false)
    private double dropoffLat;

    @Column(name = "dropoff_lng", nullable = false)
    private double dropoffLng;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private RideStatus status;

    @Column(name = "assigned_driver_id")
    private UUID assignedDriverId;

    @Column(name = "assignment_score")
    private Double assignmentScore;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected RideEntity() {}

    public RideEntity(UUID id, UUID riderId, double pickupLat, double pickupLng,
                      double dropoffLat, double dropoffLng, VehicleType vehicleType,
                      RideStatus status, UUID assignedDriverId, Double assignmentScore,
                      String failureReason, Instant requestedAt, Instant assignedAt,
                      Instant failedAt) {
        this.id               = id;
        this.riderId          = riderId;
        this.pickupLat        = pickupLat;
        this.pickupLng        = pickupLng;
        this.dropoffLat       = dropoffLat;
        this.dropoffLng       = dropoffLng;
        this.vehicleType      = vehicleType;
        this.status           = status;
        this.assignedDriverId = assignedDriverId;
        this.assignmentScore  = assignmentScore;
        this.failureReason    = failureReason;
        this.requestedAt      = requestedAt;
        this.assignedAt       = assignedAt;
        this.failedAt         = failedAt;
    }

    public UUID        getId()               { return id; }
    public UUID        getRiderId()          { return riderId; }
    public double      getPickupLat()        { return pickupLat; }
    public double      getPickupLng()        { return pickupLng; }
    public double      getDropoffLat()       { return dropoffLat; }
    public double      getDropoffLng()       { return dropoffLng; }
    public VehicleType getVehicleType()      { return vehicleType; }
    public RideStatus  getStatus()           { return status; }
    public UUID        getAssignedDriverId() { return assignedDriverId; }
    public Double      getAssignmentScore()  { return assignmentScore; }
    public String      getFailureReason()    { return failureReason; }
    public Instant     getRequestedAt()      { return requestedAt; }
    public Instant     getAssignedAt()       { return assignedAt; }
    public Instant     getFailedAt()         { return failedAt; }
    public int         getVersion()          { return version; }
    public Instant     getCreatedAt()        { return createdAt; }
    public Instant     getUpdatedAt()        { return updatedAt; }
}
