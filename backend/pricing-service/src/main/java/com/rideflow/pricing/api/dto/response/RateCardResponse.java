package com.rideflow.pricing.api.dto.response;

import com.rideflow.pricing.domain.model.RateCard;

import java.math.BigDecimal;

/** Read-only view of a rate card (the tunable inputs to the fare formula). */
public record RateCardResponse(
        String     vehicleType,
        String     currency,
        BigDecimal baseFare,
        BigDecimal perKm,
        BigDecimal perMinute,
        BigDecimal minimumFare,
        BigDecimal bookingFee) {

    public static RateCardResponse from(RateCard c) {
        return new RateCardResponse(
                c.vehicleType().name(),
                c.currency(),
                c.baseFare().amount(),
                c.perKm().amount(),
                c.perMinute().amount(),
                c.minimumFare().amount(),
                c.bookingFee().amount());
    }
}
