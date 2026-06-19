package com.rideflow.pricing.infrastructure.persistence.jpa.entity;

import com.rideflow.pricing.domain.model.VehicleType;

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
 * JPA mapping for {@code pricing.quotes}. Stores the full fare breakdown so a
 * price can be explained after the fact, independent of how rate cards or surge
 * later change.
 */
@Entity
@Table(name = "quotes", schema = "pricing")
public class QuoteEntity {

    @Id
    private UUID id;

    @Column(name = "ride_id")
    private UUID rideId;

    @Column(name = "rider_id")
    private UUID riderId;

    @Column(name = "pickup_lat", nullable = false)  private double pickupLat;
    @Column(name = "pickup_lng", nullable = false)  private double pickupLng;
    @Column(name = "dropoff_lat", nullable = false) private double dropoffLat;
    @Column(name = "dropoff_lng", nullable = false) private double dropoffLng;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "base_fare",        nullable = false) private BigDecimal baseFare;
    @Column(name = "distance_fare",    nullable = false) private BigDecimal distanceFare;
    @Column(name = "time_fare",        nullable = false) private BigDecimal timeFare;
    @Column(name = "subtotal",         nullable = false) private BigDecimal subtotal;
    @Column(name = "surge_multiplier", nullable = false) private BigDecimal surgeMultiplier;
    @Column(name = "surged_subtotal",  nullable = false) private BigDecimal surgedSubtotal;
    @Column(name = "booking_fee",      nullable = false) private BigDecimal bookingFee;
    @Column(name = "total",            nullable = false) private BigDecimal total;

    @Column(name = "est_distance_km",  nullable = false) private double estDistanceKm;
    @Column(name = "est_duration_min", nullable = false) private double estDurationMin;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected QuoteEntity() {}

    public QuoteEntity(UUID id, UUID rideId, UUID riderId,
                       double pickupLat, double pickupLng, double dropoffLat, double dropoffLng,
                       VehicleType vehicleType, String currency,
                       BigDecimal baseFare, BigDecimal distanceFare, BigDecimal timeFare,
                       BigDecimal subtotal, BigDecimal surgeMultiplier, BigDecimal surgedSubtotal,
                       BigDecimal bookingFee, BigDecimal total,
                       double estDistanceKm, double estDurationMin,
                       Instant validUntil, Instant createdAt) {
        this.id              = id;
        this.rideId          = rideId;
        this.riderId         = riderId;
        this.pickupLat       = pickupLat;
        this.pickupLng       = pickupLng;
        this.dropoffLat      = dropoffLat;
        this.dropoffLng      = dropoffLng;
        this.vehicleType     = vehicleType;
        this.currency        = currency;
        this.baseFare        = baseFare;
        this.distanceFare    = distanceFare;
        this.timeFare        = timeFare;
        this.subtotal        = subtotal;
        this.surgeMultiplier = surgeMultiplier;
        this.surgedSubtotal  = surgedSubtotal;
        this.bookingFee      = bookingFee;
        this.total           = total;
        this.estDistanceKm   = estDistanceKm;
        this.estDurationMin  = estDurationMin;
        this.validUntil      = validUntil;
        this.createdAt       = createdAt;
    }

    public UUID        getId()              { return id; }
    public UUID        getRideId()          { return rideId; }
    public UUID        getRiderId()         { return riderId; }
    public double      getPickupLat()       { return pickupLat; }
    public double      getPickupLng()       { return pickupLng; }
    public double      getDropoffLat()      { return dropoffLat; }
    public double      getDropoffLng()      { return dropoffLng; }
    public VehicleType getVehicleType()     { return vehicleType; }
    public String      getCurrency()        { return currency; }
    public BigDecimal  getBaseFare()        { return baseFare; }
    public BigDecimal  getDistanceFare()    { return distanceFare; }
    public BigDecimal  getTimeFare()        { return timeFare; }
    public BigDecimal  getSubtotal()        { return subtotal; }
    public BigDecimal  getSurgeMultiplier() { return surgeMultiplier; }
    public BigDecimal  getSurgedSubtotal()  { return surgedSubtotal; }
    public BigDecimal  getBookingFee()      { return bookingFee; }
    public BigDecimal  getTotal()           { return total; }
    public double      getEstDistanceKm()   { return estDistanceKm; }
    public double      getEstDurationMin()  { return estDurationMin; }
    public Instant     getValidUntil()      { return validUntil; }
    public Instant     getCreatedAt()       { return createdAt; }
}
