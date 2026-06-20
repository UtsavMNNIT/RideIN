package com.rideflow.rider.infrastructure.persistence.jpa.entity;

import com.rideflow.rider.domain.model.RideStatus;
import com.rideflow.rider.domain.model.VehicleType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA mapping for {@code rider.rides}. The columns past {@code status} are the
 * read-projection fields populated by consuming downstream events.
 */
@Entity
@Table(name = "rides", schema = "rider")
public class RideEntity {

    @Id
    private UUID id;

    @Column(name = "rider_id", nullable = false)
    private UUID riderId;

    @Column(name = "pickup_lat", nullable = false)  private double pickupLat;
    @Column(name = "pickup_lng", nullable = false)  private double pickupLng;
    @Column(name = "dropoff_lat", nullable = false) private double dropoffLat;
    @Column(name = "dropoff_lng", nullable = false) private double dropoffLng;

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

    @Column(name = "match_score")
    private Double matchScore;

    @Column(name = "fare_total")
    private BigDecimal fareTotal;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "final_distance_meters")
    private Integer finalDistanceMeters;

    @Column(name = "final_duration_seconds")
    private Integer finalDurationSeconds;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RideEntity() {}

    public RideEntity(UUID id, UUID riderId, double pickupLat, double pickupLng,
                      double dropoffLat, double dropoffLng, VehicleType vehicleType,
                      RideStatus status, UUID assignedDriverId, Double matchScore,
                      BigDecimal fareTotal, String currency,
                      Integer finalDistanceMeters, Integer finalDurationSeconds,
                      Instant requestedAt, Instant createdAt, Instant updatedAt) {
        this.id                   = id;
        this.riderId              = riderId;
        this.pickupLat            = pickupLat;
        this.pickupLng            = pickupLng;
        this.dropoffLat           = dropoffLat;
        this.dropoffLng           = dropoffLng;
        this.vehicleType          = vehicleType;
        this.status               = status;
        this.assignedDriverId     = assignedDriverId;
        this.matchScore           = matchScore;
        this.fareTotal            = fareTotal;
        this.currency             = currency;
        this.finalDistanceMeters  = finalDistanceMeters;
        this.finalDurationSeconds = finalDurationSeconds;
        this.requestedAt          = requestedAt;
        this.createdAt            = createdAt;
        this.updatedAt            = updatedAt;
    }

    public UUID        getId()                   { return id; }
    public UUID        getRiderId()              { return riderId; }
    public double      getPickupLat()            { return pickupLat; }
    public double      getPickupLng()            { return pickupLng; }
    public double      getDropoffLat()           { return dropoffLat; }
    public double      getDropoffLng()           { return dropoffLng; }
    public VehicleType getVehicleType()          { return vehicleType; }
    public RideStatus  getStatus()               { return status; }
    public UUID        getAssignedDriverId()     { return assignedDriverId; }
    public Double      getMatchScore()           { return matchScore; }
    public BigDecimal  getFareTotal()            { return fareTotal; }
    public String      getCurrency()             { return currency; }
    public Integer     getFinalDistanceMeters()  { return finalDistanceMeters; }
    public Integer     getFinalDurationSeconds() { return finalDurationSeconds; }
    public Instant     getRequestedAt()          { return requestedAt; }
    public Instant     getCreatedAt()            { return createdAt; }
    public Instant     getUpdatedAt()            { return updatedAt; }
}
