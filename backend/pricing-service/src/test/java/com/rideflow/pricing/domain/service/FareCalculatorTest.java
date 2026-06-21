package com.rideflow.pricing.domain.service;

import com.rideflow.pricing.domain.model.FareBreakdown;
import com.rideflow.pricing.domain.model.Money;
import com.rideflow.pricing.domain.model.RateCard;
import com.rideflow.pricing.domain.model.TripEstimate;
import com.rideflow.pricing.domain.model.VehicleType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the fare formula:
 * subtotal = base + perKm*km + perMin*min; surge applied to subtotal only;
 * booking fee added after surge; minimum-fare floors the total.
 */
class FareCalculatorTest {

    private static final String CCY = "INR";

    private final FareCalculator calculator = new FareCalculator();

    private static Money inr(String v) {
        return Money.of(new BigDecimal(v), CCY);
    }

    /** base=50, perKm=10, perMin=2, minimum=80, booking=15 */
    private static RateCard card() {
        return new RateCard(
                VehicleType.STANDARD,
                inr("50.00"),  // base
                inr("10.00"),  // per km
                inr("2.00"),   // per minute
                inr("80.00"),  // minimum
                inr("15.00")); // booking fee
    }

    @Test
    void noSurge_computesItemizedBreakdown() {
        // 10 km, 20 min => distance 100, time 40, subtotal = 50+100+40 = 190
        FareBreakdown fare = calculator.price(card(), new TripEstimate(10.0, 20.0), BigDecimal.ONE);

        assertThat(fare.baseFare()).isEqualTo(inr("50.00"));
        assertThat(fare.distanceFare()).isEqualTo(inr("100.00"));
        assertThat(fare.timeFare()).isEqualTo(inr("40.00"));
        assertThat(fare.subtotal()).isEqualTo(inr("190.00"));
        assertThat(fare.surgeMultiplier()).isEqualByComparingTo(BigDecimal.ONE);
        // surge=1 => surgedSubtotal == subtotal
        assertThat(fare.surgedSubtotal()).isEqualTo(inr("190.00"));
        assertThat(fare.bookingFee()).isEqualTo(inr("15.00"));
        // total = 190 + 15 = 205 (above minimum)
        assertThat(fare.total()).isEqualTo(inr("205.00"));
        assertThat(fare.currency()).isEqualTo(CCY);
    }

    @Test
    void surgeApplied_beforeBookingFee_doesNotAmplifyFee() {
        // subtotal 190 * 2.0 = 380; + booking 15 = 395 (fee NOT surged)
        FareBreakdown fare = calculator.price(card(), new TripEstimate(10.0, 20.0), new BigDecimal("2.0"));

        assertThat(fare.subtotal()).isEqualTo(inr("190.00"));
        assertThat(fare.surgedSubtotal()).isEqualTo(inr("380.00"));
        // If the fee had been surged the total would be (190+15)*2 = 410.
        assertThat(fare.total()).isEqualTo(inr("395.00"));
    }

    @Test
    void belowMinimum_isFlooredToMinimumFare() {
        // tiny trip: 0.5 km, 1 min => distance 5, time 2, subtotal = 57
        // + booking 15 = 72, below minimum 80 => floored to 80
        FareBreakdown fare = calculator.price(card(), new TripEstimate(0.5, 1.0), BigDecimal.ONE);

        assertThat(fare.subtotal()).isEqualTo(inr("57.00"));
        assertThat(fare.total()).isEqualTo(inr("80.00"));
    }

    @Test
    void zeroDistanceAndTime_stillChargesBasePlusFeeOrFloor() {
        // subtotal = base 50; + booking 15 = 65, below min 80 => 80
        FareBreakdown fare = calculator.price(card(), new TripEstimate(0.0, 0.0), BigDecimal.ONE);

        assertThat(fare.distanceFare()).isEqualTo(inr("0.00"));
        assertThat(fare.timeFare()).isEqualTo(inr("0.00"));
        assertThat(fare.subtotal()).isEqualTo(inr("50.00"));
        assertThat(fare.total()).isEqualTo(inr("80.00"));
    }

    @Test
    void surgeExactlyOne_isNoOpMultiplier() {
        FareBreakdown fare = calculator.price(card(), new TripEstimate(10.0, 20.0), new BigDecimal("1.00"));
        assertThat(fare.surgedSubtotal()).isEqualTo(fare.subtotal());
    }

    @Test
    void surgePushesAboveMinimum_floorNotApplied() {
        // small trip that alone floors, but surge lifts it above minimum.
        // 1 km, 2 min => distance 10, time 4, subtotal = 64; *3 = 192; +15 = 207 > 80
        FareBreakdown fare = calculator.price(card(), new TripEstimate(1.0, 2.0), new BigDecimal("3.0"));
        assertThat(fare.subtotal()).isEqualTo(inr("64.00"));
        assertThat(fare.surgedSubtotal()).isEqualTo(inr("192.00"));
        assertThat(fare.total()).isEqualTo(inr("207.00"));
    }

    @Test
    void nullSurge_isRejected() {
        assertThatThrownBy(() -> calculator.price(card(), new TripEstimate(1.0, 1.0), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeSurge_isRejected() {
        assertThatThrownBy(() -> calculator.price(card(), new TripEstimate(1.0, 1.0), new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
