package com.rideflow.pricing.infrastructure.pricing;

import com.rideflow.pricing.application.port.out.SurgeProvider;
import com.rideflow.pricing.domain.model.GeoPoint;
import com.rideflow.pricing.domain.model.VehicleType;
import com.rideflow.pricing.infrastructure.config.PricingProperties;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Config-driven {@link SurgeProvider}: a flat baseline multiplier with optional
 * per-vehicle-type overrides, both from {@code rideflow.pricing.surge.*}.
 * Location-independent for now (the {@code pickup} argument is ignored).
 *
 * <p>This is the deliberate seam for demand-based pricing: a future
 * {@code DemandSurgeProvider} can read live supply/demand per geo-cell (from the
 * location/matching services) and key off {@code pickup}, replacing this bean
 * without any change to the fare pipeline.
 */
@Component
public class ConfigurableSurgeProvider implements SurgeProvider {

    private final BigDecimal                    defaultMultiplier;
    private final Map<VehicleType, BigDecimal>  overrides;

    public ConfigurableSurgeProvider(PricingProperties properties) {
        this.defaultMultiplier = properties.surge().defaultMultiplier();
        this.overrides         = properties.surge().overrides();
    }

    @Override
    public BigDecimal multiplierFor(VehicleType vehicleType, GeoPoint pickup) {
        return overrides.getOrDefault(vehicleType, defaultMultiplier);
    }
}
