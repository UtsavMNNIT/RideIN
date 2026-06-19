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

/**
 * JPA mapping for {@code pricing.rate_cards}. One row per vehicle type; the
 * vehicle type is the primary key. Edited by operations to retune fares without
 * a redeploy — {@code DbRateCardProvider} caches it and refreshes periodically.
 */
@Entity
@Table(name = "rate_cards", schema = "pricing")
public class RateCardEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "base_fare",    nullable = false) private BigDecimal baseFare;
    @Column(name = "per_km",       nullable = false) private BigDecimal perKm;
    @Column(name = "per_minute",   nullable = false) private BigDecimal perMinute;
    @Column(name = "minimum_fare", nullable = false) private BigDecimal minimumFare;
    @Column(name = "booking_fee",  nullable = false) private BigDecimal bookingFee;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected RateCardEntity() {}

    public VehicleType getVehicleType() { return vehicleType; }
    public String      getCurrency()    { return currency; }
    public BigDecimal  getBaseFare()    { return baseFare; }
    public BigDecimal  getPerKm()       { return perKm; }
    public BigDecimal  getPerMinute()   { return perMinute; }
    public BigDecimal  getMinimumFare() { return minimumFare; }
    public BigDecimal  getBookingFee()  { return bookingFee; }
    public Instant     getUpdatedAt()   { return updatedAt; }
}
