package com.rideflow.pricing.infrastructure.config;

import com.rideflow.pricing.domain.model.VehicleType;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Typed binding for {@code rideflow.pricing.*} (see {@code application.yml}).
 *
 * <p>The estimation knobs and surge config are bound here and injected into the
 * estimator/surge adapters. Outbox + rate-card-cache timings stay on
 * {@code @Value} in their respective beans (the relay's {@code @Scheduled} needs
 * a placeholder string, not a bound object).
 */
@ConfigurationProperties(prefix = "rideflow.pricing")
public record PricingProperties(
        String   currency,
        Estimate estimate,
        Quote    quote,
        Surge    surge
) {

    public PricingProperties {
        if (currency == null || currency.isBlank())
            throw new IllegalArgumentException("rideflow.pricing.currency is required");
        if (estimate == null) throw new IllegalArgumentException("rideflow.pricing.estimate is required");
        if (quote == null)    throw new IllegalArgumentException("rideflow.pricing.quote is required");
        if (surge == null)    throw new IllegalArgumentException("rideflow.pricing.surge is required");
    }

    /** Distance/duration approximation inputs. */
    public record Estimate(double roadFactor, double avgSpeedKmph) {
        public Estimate {
            if (roadFactor < 1.0)    throw new IllegalArgumentException("road-factor must be >= 1.0");
            if (avgSpeedKmph <= 0.0) throw new IllegalArgumentException("avg-speed-kmph must be > 0");
        }
    }

    /** How long an up-front quote is honoured. */
    public record Quote(int ttlMinutes) {
        public Quote {
            if (ttlMinutes <= 0) throw new IllegalArgumentException("quote.ttl-minutes must be > 0");
        }
    }

    /**
     * Flat-baseline surge config. {@code overrides} wins per vehicle type when
     * present; an empty map means everyone gets {@code defaultMultiplier}.
     */
    public record Surge(BigDecimal defaultMultiplier, Map<VehicleType, BigDecimal> overrides) {
        public Surge {
            if (defaultMultiplier == null || defaultMultiplier.signum() < 0)
                throw new IllegalArgumentException("surge.default-multiplier must be >= 0");
            overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
        }
    }
}
