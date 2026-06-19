package com.rideflow.pricing.api.dto.response;

import com.rideflow.pricing.domain.model.FareBreakdown;

import java.math.BigDecimal;

/**
 * Itemized fare, as returned to clients. Every term of the formula is exposed so
 * the rider app can render a transparent receipt without re-deriving anything.
 */
public record FareBreakdownResponse(
        String     currency,
        BigDecimal baseFare,
        BigDecimal distanceFare,
        BigDecimal timeFare,
        BigDecimal subtotal,
        BigDecimal surgeMultiplier,
        BigDecimal surgedSubtotal,
        BigDecimal bookingFee,
        BigDecimal total) {

    public static FareBreakdownResponse from(FareBreakdown f) {
        return new FareBreakdownResponse(
                f.currency(),
                f.baseFare().amount(),
                f.distanceFare().amount(),
                f.timeFare().amount(),
                f.subtotal().amount(),
                f.surgeMultiplier(),
                f.surgedSubtotal().amount(),
                f.bookingFee().amount(),
                f.total().amount());
    }
}
