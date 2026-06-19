package com.rideflow.pricing.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The full, itemized result of pricing a trip — every term of the fare formula
 * kept separate for transparency, receipts, audit, and dispute handling.
 *
 * <pre>
 *   subtotal        = baseFare + distanceFare + timeFare
 *   surgedSubtotal  = subtotal × surgeMultiplier
 *   total           = max(surgedSubtotal + bookingFee, minimumFare)
 * </pre>
 *
 * Computed once by {@link com.rideflow.pricing.domain.service.FareCalculator};
 * persisted verbatim on the quote so a fare can always be explained after the
 * fact even if rate cards or surge later change.
 */
public record FareBreakdown(
        Money       baseFare,
        Money       distanceFare,
        Money       timeFare,
        Money       subtotal,
        BigDecimal  surgeMultiplier,
        Money       surgedSubtotal,
        Money       bookingFee,
        Money       total
) {

    public FareBreakdown {
        Objects.requireNonNull(baseFare,       "baseFare");
        Objects.requireNonNull(distanceFare,   "distanceFare");
        Objects.requireNonNull(timeFare,       "timeFare");
        Objects.requireNonNull(subtotal,       "subtotal");
        Objects.requireNonNull(surgeMultiplier,"surgeMultiplier");
        Objects.requireNonNull(surgedSubtotal, "surgedSubtotal");
        Objects.requireNonNull(bookingFee,     "bookingFee");
        Objects.requireNonNull(total,          "total");
        if (surgeMultiplier.signum() < 0) {
            throw new IllegalArgumentException("surgeMultiplier must be >= 0");
        }
    }

    public String currency() {
        return total.currency();
    }
}
