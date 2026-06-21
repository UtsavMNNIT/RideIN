package com.rideflow.pricing.infrastructure.pricing;

import com.rideflow.pricing.application.port.out.DriverSupplyProvider;
import com.rideflow.pricing.domain.model.GeoPoint;
import com.rideflow.pricing.domain.model.VehicleType;
import com.rideflow.pricing.infrastructure.config.PricingProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the demand-based surge provider built on top of the configured
 * baseline. Uses a hand-rolled {@link DriverSupplyProvider} fake (no Mockito).
 */
class DemandSurgeProviderTest {

    private static final GeoPoint PICKUP = new GeoPoint(12.97, 77.59);

    /** Fake supply provider: returns a fixed count, or throws if {@code fail}. */
    private static final class FakeSupply implements DriverSupplyProvider {
        private final int count;
        private final boolean fail;
        FakeSupply(int count) { this(count, false); }
        FakeSupply(int count, boolean fail) { this.count = count; this.fail = fail; }
        @Override
        public int availableDriversNear(VehicleType vehicleType, GeoPoint pickup, int radiusMeters) {
            if (fail) throw new RuntimeException("supply source down");
            return count;
        }
    }

    private static PricingProperties props(BigDecimal baseline,
                                           Map<VehicleType, BigDecimal> overrides,
                                           boolean dynamicEnabled,
                                           int targetSupply,
                                           BigDecimal maxMultiplier) {
        return new PricingProperties(
                "INR",
                new PricingProperties.Estimate(1.3, 30.0),
                new PricingProperties.Quote(5),
                new PricingProperties.Surge(
                        baseline,
                        overrides,
                        new PricingProperties.Surge.Dynamic(dynamicEnabled, 3000, targetSupply, maxMultiplier)));
    }

    private static DemandSurgeProvider provider(PricingProperties p, DriverSupplyProvider supply) {
        return new DemandSurgeProvider(new ConfigurableSurgeProvider(p), supply, p);
    }

    @Test
    void supplyAtOrAboveTarget_noSurge() {
        PricingProperties p = props(new BigDecimal("1.00"), Map.of(), true, 5, new BigDecimal("3.00"));
        DemandSurgeProvider sut = provider(p, new FakeSupply(5));
        assertThat(sut.multiplierFor(VehicleType.STANDARD, PICKUP)).isEqualByComparingTo("1.00");

        // Above target also => 1.00
        DemandSurgeProvider sut2 = provider(p, new FakeSupply(20));
        assertThat(sut2.multiplierFor(VehicleType.STANDARD, PICKUP)).isEqualByComparingTo("1.00");
    }

    @Test
    void zeroSupply_reachesMaxMultiplier() {
        PricingProperties p = props(new BigDecimal("1.00"), Map.of(), true, 5, new BigDecimal("3.00"));
        DemandSurgeProvider sut = provider(p, new FakeSupply(0));
        assertThat(sut.multiplierFor(VehicleType.STANDARD, PICKUP)).isEqualByComparingTo("3.00");
    }

    @Test
    void midSupply_rampsLinearly() {
        // target 5, max 3.00 (span 2.00). supply 2 => shortfall (5-2)/5 = 0.6
        // multiplier = 1 + 2.00*0.6 = 2.20
        PricingProperties p = props(new BigDecimal("1.00"), Map.of(), true, 5, new BigDecimal("3.00"));
        DemandSurgeProvider sut = provider(p, new FakeSupply(2));
        assertThat(sut.multiplierFor(VehicleType.STANDARD, PICKUP)).isEqualByComparingTo("2.20");
    }

    @Test
    void supplyLookupThrows_fallsBackToConfiguredBaseline() {
        PricingProperties p = props(new BigDecimal("1.50"), Map.of(), true, 5, new BigDecimal("3.00"));
        DemandSurgeProvider sut = provider(p, new FakeSupply(0, true));
        // Despite zero-ish demand path, the exception forces fallback to baseline 1.50.
        assertThat(sut.multiplierFor(VehicleType.STANDARD, PICKUP)).isEqualByComparingTo("1.50");
    }

    @Test
    void configuredBaseline_floorsTheDynamicResult() {
        // Plenty of supply => demand would give 1.00, but baseline 1.80 floors it.
        PricingProperties p = props(new BigDecimal("1.80"), Map.of(), true, 5, new BigDecimal("3.00"));
        DemandSurgeProvider sut = provider(p, new FakeSupply(50));
        assertThat(sut.multiplierFor(VehicleType.STANDARD, PICKUP)).isEqualByComparingTo("1.80");
    }

    @Test
    void perVehicleOverride_actsAsFloor() {
        PricingProperties p = props(
                new BigDecimal("1.00"),
                Map.of(VehicleType.PREMIUM, new BigDecimal("2.50")),
                true, 5, new BigDecimal("3.00"));
        DemandSurgeProvider sut = provider(p, new FakeSupply(50)); // demand=1.00
        // PREMIUM override 2.50 floors the 1.00 demand result.
        assertThat(sut.multiplierFor(VehicleType.PREMIUM, PICKUP)).isEqualByComparingTo("2.50");
        // STANDARD has no override => demand 1.00 wins.
        assertThat(sut.multiplierFor(VehicleType.STANDARD, PICKUP)).isEqualByComparingTo("1.00");
    }

    @Test
    void dynamicDisabled_returnsConfiguredBaselineWithoutConsultingSupply() {
        PricingProperties p = props(new BigDecimal("1.25"), Map.of(), false, 5, new BigDecimal("3.00"));
        DemandSurgeProvider sut = provider(p, new FakeSupply(0, true)); // would throw if consulted
        assertThat(sut.multiplierFor(VehicleType.STANDARD, PICKUP)).isEqualByComparingTo("1.25");
    }

    @Test
    void nullPickup_returnsConfiguredBaseline() {
        PricingProperties p = props(new BigDecimal("1.40"), Map.of(), true, 5, new BigDecimal("3.00"));
        DemandSurgeProvider sut = provider(p, new FakeSupply(0, true)); // would throw if consulted
        assertThat(sut.multiplierFor(VehicleType.STANDARD, null)).isEqualByComparingTo("1.40");
    }
}
