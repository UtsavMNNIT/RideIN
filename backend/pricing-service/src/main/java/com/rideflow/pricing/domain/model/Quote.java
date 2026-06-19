package com.rideflow.pricing.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A priced trip. Two flavours, same shape:
 *
 * <ul>
 *   <li><b>Up-front quote</b> — a rider asks "how much?" before committing.
 *       {@link #rideId} is {@code null}; the quote is honoured until
 *       {@link #validUntil}.</li>
 *   <li><b>Ride quote</b> — the authoritative fare for an actual ride, computed
 *       when {@code rider.RideRequested} arrives. {@link #rideId} is set and is
 *       unique (one quote per ride), making a redelivered event a no-op.</li>
 * </ul>
 *
 * <p>Immutable. The {@link FareBreakdown} is frozen onto the quote so the price
 * can always be explained later even if rate cards or surge move.
 */
public record Quote(
        UUID         id,
        UUID         rideId,      // null for up-front estimates
        UUID         riderId,     // null for anonymous up-front estimates
        GeoPoint     pickup,
        GeoPoint     dropoff,
        VehicleType  vehicleType,
        TripEstimate estimate,
        FareBreakdown fare,
        Instant      validUntil,
        Instant      createdAt
) {

    public Quote {
        Objects.requireNonNull(id,          "id");
        Objects.requireNonNull(pickup,      "pickup");
        Objects.requireNonNull(dropoff,     "dropoff");
        Objects.requireNonNull(vehicleType, "vehicleType");
        Objects.requireNonNull(estimate,    "estimate");
        Objects.requireNonNull(fare,        "fare");
        Objects.requireNonNull(validUntil,  "validUntil");
        Objects.requireNonNull(createdAt,   "createdAt");
    }

    /** Anonymous/identified up-front estimate, not tied to a ride. */
    public static Quote upfront(UUID id, UUID riderId, GeoPoint pickup, GeoPoint dropoff,
                                VehicleType vehicleType, TripEstimate estimate,
                                FareBreakdown fare, Instant createdAt, Instant validUntil) {
        return new Quote(id, null, riderId, pickup, dropoff, vehicleType,
                estimate, fare, validUntil, createdAt);
    }

    /** Authoritative fare bound to a concrete ride. */
    public static Quote forRide(UUID id, UUID rideId, UUID riderId, GeoPoint pickup, GeoPoint dropoff,
                                VehicleType vehicleType, TripEstimate estimate,
                                FareBreakdown fare, Instant createdAt, Instant validUntil) {
        Objects.requireNonNull(rideId, "rideId");
        return new Quote(id, rideId, riderId, pickup, dropoff, vehicleType,
                estimate, fare, validUntil, createdAt);
    }

    public boolean isExpiredAt(Instant when) {
        return when.isAfter(validUntil);
    }
}
