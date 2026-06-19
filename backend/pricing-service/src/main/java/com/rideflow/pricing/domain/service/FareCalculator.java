package com.rideflow.pricing.domain.service;

import com.rideflow.pricing.domain.model.FareBreakdown;
import com.rideflow.pricing.domain.model.Money;
import com.rideflow.pricing.domain.model.RateCard;
import com.rideflow.pricing.domain.model.TripEstimate;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * The heart of the service: turns a rate card, a trip estimate, and a surge
 * multiplier into an itemized {@link FareBreakdown}.
 *
 * <h3>Formula</h3>
 * <pre>
 *   distanceFare    = perKm      × estimatedKm
 *   timeFare        = perMinute  × estimatedMinutes
 *   subtotal        = baseFare + distanceFare + timeFare
 *   surgedSubtotal  = subtotal  × surgeMultiplier
 *   total           = max(surgedSubtotal + bookingFee, minimumFare)
 * </pre>
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li><b>Pure.</b> No I/O, no clock, no Spring state — deterministic in its
 *       inputs, which makes it trivially unit-testable and safe to call from any
 *       thread. It's a {@code @Component} only so it can be injected.</li>
 *   <li><b>{@link Money} throughout.</b> Every term is scale-2 {@link BigDecimal}
 *       under the hood; fares never drift by a rounding cent and a cross-currency
 *       mistake fails loudly rather than silently mis-charging.</li>
 *   <li><b>Surge is the last multiplicative step before fixed fees.</b> The
 *       booking fee is a flat add-on that surge does not amplify; the minimum
 *       fare floors the whole thing. This ordering is the policy knob most likely
 *       to change — it lives here, in one place.</li>
 * </ul>
 */
@Component
public class FareCalculator {

    public FareBreakdown price(RateCard card, TripEstimate trip, BigDecimal surgeMultiplier) {
        if (surgeMultiplier == null || surgeMultiplier.signum() < 0) {
            throw new IllegalArgumentException("surgeMultiplier must be a non-negative number");
        }

        Money baseFare     = card.baseFare();
        Money distanceFare = card.perKm().multiply(BigDecimal.valueOf(trip.distanceKm()));
        Money timeFare     = card.perMinute().multiply(BigDecimal.valueOf(trip.durationMinutes()));

        Money subtotal       = baseFare.add(distanceFare).add(timeFare);
        Money surgedSubtotal = subtotal.multiply(surgeMultiplier);

        // Booking fee is added after surge (surge does not amplify the flat fee),
        // then the minimum fare acts as a floor on the payable total.
        Money beforeFloor = surgedSubtotal.add(card.bookingFee());
        Money total       = beforeFloor.max(card.minimumFare());

        return new FareBreakdown(
                baseFare,
                distanceFare,
                timeFare,
                subtotal,
                surgeMultiplier,
                surgedSubtotal,
                card.bookingFee(),
                total);
    }
}
