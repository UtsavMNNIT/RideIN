package com.rideflow.matching.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Typed binding for {@code rideflow.dispatch.*} (see {@code application.yml}).
 *
 * <p>Lock settings stay on {@code @Value} in the Redisson beans — they're
 * needed before this record is constructed during context refresh; everything
 * else (the radius ladder, candidate sizing, scorer weights) is bound here.
 */
@ConfigurationProperties(prefix = "rideflow.dispatch")
public record DispatchProperties(
        int           candidateLimit,
        long          candidateStalenessMs,
        int           maxRedispatches,
        List<Integer> attempts,
        Weight        weight
) {

    public DispatchProperties {
        if (candidateLimit <= 0)        throw new IllegalArgumentException("candidate-limit must be > 0");
        if (candidateStalenessMs <= 0)  throw new IllegalArgumentException("candidate-staleness-ms must be > 0");
        if (maxRedispatches < 0)        throw new IllegalArgumentException("max-redispatches must be >= 0");
        if (attempts == null || attempts.isEmpty())
            throw new IllegalArgumentException("dispatch attempts (radius ladder) must not be empty");
        if (weight == null) throw new IllegalArgumentException("dispatch weights required");
    }

    /** Scorer blend weights. Need not sum to 1.0 — the scorer normalises. */
    public record Weight(double distance, double heading) {}
}
