package com.rideflow.pricing.domain.model;

import java.util.Objects;

/**
 * Per-vehicle-type pricing parameters — the tunable inputs to the fare formula.
 *
 * <p>Lives in the {@code pricing.rate_cards} table (not config) so operations can
 * retune fares without a redeploy; loaded and cached behind
 * {@link com.rideflow.pricing.application.port.out.RateCardProvider}.
 *
 * <p>Monetary fields are {@link Money}; {@link #perKm} / {@link #perMinute} are
 * money-per-unit rates that, multiplied by an estimated distance/duration, yield
 * the distance and time legs of the fare.
 */
public record RateCard(
        VehicleType vehicleType,
        Money       baseFare,
        Money       perKm,
        Money       perMinute,
        Money       minimumFare,
        Money       bookingFee
) {

    public RateCard {
        Objects.requireNonNull(vehicleType, "vehicleType");
        Objects.requireNonNull(baseFare,    "baseFare");
        Objects.requireNonNull(perKm,       "perKm");
        Objects.requireNonNull(perMinute,   "perMinute");
        Objects.requireNonNull(minimumFare, "minimumFare");
        Objects.requireNonNull(bookingFee,  "bookingFee");
        // Every leg of a card must be in the same currency, or the calculator's
        // Money arithmetic would reject the trip mid-computation.
        String ccy = baseFare.currency();
        requireSameCurrency(ccy, perKm, "perKm");
        requireSameCurrency(ccy, perMinute, "perMinute");
        requireSameCurrency(ccy, minimumFare, "minimumFare");
        requireSameCurrency(ccy, bookingFee, "bookingFee");
    }

    /** ISO currency of this card (all legs share it). */
    public String currency() {
        return baseFare.currency();
    }

    private static void requireSameCurrency(String expected, Money m, String field) {
        if (!expected.equals(m.currency())) {
            throw new IllegalArgumentException(
                    "rate-card currency mismatch on " + field + ": " + m.currency() + " != " + expected);
        }
    }
}
