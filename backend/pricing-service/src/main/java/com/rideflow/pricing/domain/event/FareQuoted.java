package com.rideflow.pricing.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event: the authoritative fare for a ride has been computed.
 *
 * <p>Published on {@code pricing.fare-quoted} (via the transactional outbox)
 * after a {@code rider.RideRequested} is priced. Downstream consumers
 * (payments, the rider app's fare display, analytics) read the full breakdown so
 * they never have to re-derive the price.
 *
 * <p>Money fields are plain {@link BigDecimal} amounts in {@link #currency} —
 * the wire contract carries numbers, not the internal {@code Money} type.
 */
public record FareQuoted(
        UUID       quoteId,
        UUID       rideId,
        UUID       riderId,
        String     vehicleType,
        String     currency,
        BigDecimal baseFare,
        BigDecimal distanceFare,
        BigDecimal timeFare,
        BigDecimal subtotal,
        BigDecimal surgeMultiplier,
        BigDecimal surgedSubtotal,
        BigDecimal bookingFee,
        BigDecimal total,
        double     estDistanceKm,
        double     estDurationMin,
        Instant    validUntil,
        Instant    quotedAt
) {}
