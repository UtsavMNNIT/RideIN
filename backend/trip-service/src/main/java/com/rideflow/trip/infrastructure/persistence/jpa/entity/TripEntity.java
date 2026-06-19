package com.rideflow.trip.infrastructure.persistence.jpa.entity;

import com.rideflow.trip.domain.model.CancelledBy;
import com.rideflow.trip.domain.model.Trip;
import com.rideflow.trip.domain.model.TripStatus;

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
 * JPA mapping for {@code trip.trips}. {@code created_at}/{@code updated_at} are
 * DB-managed (default + trigger). {@code @Version} gives optimistic locking so
 * concurrent transitions (e.g. accept vs the expiry sweeper) can't both commit.
 */
@Entity
@Table(name = "trips", schema = "trip")
public class TripEntity {

    @Id
    private UUID id;

    @Column(name = "rider_id", nullable = false)
    private UUID riderId;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "pickup_lat", nullable = false)  private double pickupLat;
    @Column(name = "pickup_lng", nullable = false)  private double pickupLng;
    @Column(name = "dropoff_lat", nullable = false) private double dropoffLat;
    @Column(name = "dropoff_lng", nullable = false) private double dropoffLng;

    @Column(name = "vehicle_type", nullable = false)
    private String vehicleType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private TripStatus status;

    @Column(name = "match_score")
    private Double matchScore;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "cancelled_by")
    private CancelledBy cancelledBy;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "final_distance_meters")
    private Integer finalDistanceMeters;

    @Column(name = "final_duration_seconds")
    private Integer finalDurationSeconds;

    @Column(name = "offered_at", nullable = false)
    private Instant offeredAt;

    @Column(name = "offer_expires_at", nullable = false)
    private Instant offerExpiresAt;

    @Column(name = "accepted_at")  private Instant acceptedAt;
    @Column(name = "arrived_at")   private Instant arrivedAt;
    @Column(name = "started_at")   private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "rejected_at")  private Instant rejectedAt;
    @Column(name = "cancelled_at") private Instant cancelledAt;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected TripEntity() {}

    public TripEntity(UUID id, UUID riderId, UUID driverId,
                      double pickupLat, double pickupLng, double dropoffLat, double dropoffLng,
                      String vehicleType, TripStatus status, Double matchScore,
                      String rejectReason, CancelledBy cancelledBy, String cancelReason,
                      Integer finalDistanceMeters, Integer finalDurationSeconds,
                      Instant offeredAt, Instant offerExpiresAt, Instant acceptedAt, Instant arrivedAt,
                      Instant startedAt, Instant completedAt, Instant rejectedAt, Instant cancelledAt) {
        this.id                   = id;
        this.riderId              = riderId;
        this.driverId             = driverId;
        this.pickupLat            = pickupLat;
        this.pickupLng            = pickupLng;
        this.dropoffLat           = dropoffLat;
        this.dropoffLng           = dropoffLng;
        this.vehicleType          = vehicleType;
        this.status               = status;
        this.matchScore           = matchScore;
        this.rejectReason         = rejectReason;
        this.cancelledBy          = cancelledBy;
        this.cancelReason         = cancelReason;
        this.finalDistanceMeters  = finalDistanceMeters;
        this.finalDurationSeconds = finalDurationSeconds;
        this.offeredAt            = offeredAt;
        this.offerExpiresAt       = offerExpiresAt;
        this.acceptedAt           = acceptedAt;
        this.arrivedAt            = arrivedAt;
        this.startedAt            = startedAt;
        this.completedAt          = completedAt;
        this.rejectedAt           = rejectedAt;
        this.cancelledAt          = cancelledAt;
    }

    /**
     * Copy the mutable lifecycle fields from a transitioned domain {@link Trip}
     * onto this <em>managed</em> entity, so Hibernate dirty-checks the update and
     * bumps {@code @Version} (rather than merging a fresh version-0 instance,
     * which would spuriously fail the optimistic-lock check on every update).
     * Identity/offer-time fields never change and are not touched.
     */
    public void applyState(Trip t) {
        this.status               = t.status();
        this.rejectReason         = t.rejectReason();
        this.cancelledBy          = t.cancelledBy();
        this.cancelReason         = t.cancelReason();
        this.finalDistanceMeters  = t.finalDistanceMeters();
        this.finalDurationSeconds = t.finalDurationSeconds();
        this.acceptedAt           = t.acceptedAt();
        this.arrivedAt            = t.arrivedAt();
        this.startedAt            = t.startedAt();
        this.completedAt          = t.completedAt();
        this.rejectedAt           = t.rejectedAt();
        this.cancelledAt          = t.cancelledAt();
    }

    public UUID        getId()                   { return id; }
    public UUID        getRiderId()              { return riderId; }
    public UUID        getDriverId()             { return driverId; }
    public double      getPickupLat()            { return pickupLat; }
    public double      getPickupLng()            { return pickupLng; }
    public double      getDropoffLat()           { return dropoffLat; }
    public double      getDropoffLng()           { return dropoffLng; }
    public String      getVehicleType()          { return vehicleType; }
    public TripStatus  getStatus()               { return status; }
    public Double      getMatchScore()           { return matchScore; }
    public String      getRejectReason()         { return rejectReason; }
    public CancelledBy getCancelledBy()          { return cancelledBy; }
    public String      getCancelReason()         { return cancelReason; }
    public Integer     getFinalDistanceMeters()  { return finalDistanceMeters; }
    public Integer     getFinalDurationSeconds() { return finalDurationSeconds; }
    public Instant     getOfferedAt()            { return offeredAt; }
    public Instant     getOfferExpiresAt()       { return offerExpiresAt; }
    public Instant     getAcceptedAt()           { return acceptedAt; }
    public Instant     getArrivedAt()            { return arrivedAt; }
    public Instant     getStartedAt()            { return startedAt; }
    public Instant     getCompletedAt()          { return completedAt; }
    public Instant     getRejectedAt()           { return rejectedAt; }
    public Instant     getCancelledAt()          { return cancelledAt; }
    public int         getVersion()              { return version; }
}
