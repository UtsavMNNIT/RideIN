package com.rideflow.pricing.infrastructure.pricing;

import com.rideflow.pricing.application.port.out.DriverSupplyProvider;
import com.rideflow.pricing.application.port.out.SurgeProvider;
import com.rideflow.pricing.domain.model.GeoPoint;
import com.rideflow.pricing.domain.model.VehicleType;
import com.rideflow.pricing.infrastructure.config.PricingProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Demand-based {@link SurgeProvider}: scales the multiplier up as live driver
 * supply near the pickup falls. This is the provider the fare pipeline uses by
 * default (it is {@link Primary}); {@link ConfigurableSurgeProvider} stays as the
 * configured baseline and the graceful-degradation fallback.
 *
 * <h3>Policy</h3>
 * <pre>
 *   supply &gt;= targetSupply        → 1.00 (no surge — plenty of drivers)
 *   0 &lt;= supply &lt; targetSupply    → linear ramp 1.00 … maxMultiplier
 *   supply == 0                   → maxMultiplier
 * </pre>
 * The result is floored by the configured baseline ({@code surge.default-multiplier}
 * / per-vehicle overrides), so an ops-set manual surge can only ever raise the
 * price, never lower a demand-driven one.
 *
 * <h3>Resilience</h3>
 * Surge sits on the synchronous quote path. If dynamic surge is disabled, the
 * pickup is unknown, or the supply lookup throws/times out, we return the
 * configured baseline — a quote is never failed or stalled by a surge miss.
 */
@Component
@Primary
public class DemandSurgeProvider implements SurgeProvider {

    private static final Logger log = LoggerFactory.getLogger(DemandSurgeProvider.class);

    private final ConfigurableSurgeProvider          baseline;
    private final DriverSupplyProvider               supply;
    private final PricingProperties.Surge.Dynamic    cfg;

    public DemandSurgeProvider(ConfigurableSurgeProvider baseline,
                               DriverSupplyProvider supply,
                               PricingProperties properties) {
        this.baseline = baseline;
        this.supply   = supply;
        this.cfg      = properties.surge().dynamic();
    }

    @Override
    public BigDecimal multiplierFor(VehicleType vehicleType, GeoPoint pickup) {
        BigDecimal configured = baseline.multiplierFor(vehicleType, pickup);

        if (!cfg.enabled() || pickup == null) {
            return configured;
        }

        int available;
        try {
            available = supply.availableDriversNear(vehicleType, pickup, cfg.radiusMeters());
        } catch (RuntimeException e) {
            log.warn("Supply lookup failed for {} near {}; falling back to configured surge {} ({})",
                    vehicleType, pickup, configured, e.toString());
            return configured;
        }

        BigDecimal demand = fromSupply(available);
        BigDecimal effective = demand.max(configured);
        log.debug("Dynamic surge {} = max(demand {} @supply={}, configured {})",
                effective, demand, available, configured);
        return effective;
    }

    private BigDecimal fromSupply(int available) {
        int target = cfg.targetSupply();
        if (available >= target) {
            return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        }
        // Linear ramp: fraction of the supply shortfall, 0 at target → 1 at zero supply.
        BigDecimal shortfall = BigDecimal.valueOf((long) target - available)
                .divide(BigDecimal.valueOf(target), 4, RoundingMode.HALF_UP);
        BigDecimal span = cfg.maxMultiplier().subtract(BigDecimal.ONE);
        BigDecimal multiplier = BigDecimal.ONE.add(span.multiply(shortfall));
        return multiplier.min(cfg.maxMultiplier()).setScale(2, RoundingMode.HALF_UP);
    }
}
