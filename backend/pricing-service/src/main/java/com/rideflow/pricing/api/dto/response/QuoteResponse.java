package com.rideflow.pricing.api.dto.response;

import com.rideflow.pricing.domain.model.Quote;

import java.time.Instant;
import java.util.UUID;

/** A quote as returned by the quote API. */
public record QuoteResponse(
        UUID                  quoteId,
        UUID                  rideId,
        UUID                  riderId,
        String                vehicleType,
        double                estDistanceKm,
        double                estDurationMin,
        FareBreakdownResponse fare,
        Instant               validUntil,
        Instant               createdAt) {

    public static QuoteResponse from(Quote q) {
        return new QuoteResponse(
                q.id(),
                q.rideId(),
                q.riderId(),
                q.vehicleType().name(),
                q.estimate().distanceKm(),
                q.estimate().durationMinutes(),
                FareBreakdownResponse.from(q.fare()),
                q.validUntil(),
                q.createdAt());
    }
}
