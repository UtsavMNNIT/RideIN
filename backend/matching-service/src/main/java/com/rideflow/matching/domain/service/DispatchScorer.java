package com.rideflow.matching.domain.service;

import com.rideflow.matching.domain.model.DispatchCandidate;
import com.rideflow.matching.domain.model.GeoPoint;
import com.rideflow.matching.infrastructure.config.DispatchProperties;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * The matching algorithm: rank nearby candidates and pick the best.
 *
 * <h2>Why not "just nearest"?</h2>
 * Straight-line distance is the dominant factor, but a driver 200m away
 * <em>heading the wrong way down a divided highway</em> has a worse real ETA
 * than one 400m away pointed straight at the pickup. Until we have a road-graph
 * ETA service, heading alignment is a cheap, high-signal proxy that captures
 * "is this driver actually getting closer."
 *
 * <h2>Score</h2>
 * Each term is normalised to {@code [0,1]} where higher is better, then blended
 * by the configured weights (default 0.70 distance / 0.30 heading):
 *
 * <pre>
 *   distanceScore = 1 - (distance / searchRadius)          // closer ⇒ higher
 *   headingScore  = (1 + cos(Δ)) / 2                        // aligned ⇒ higher
 *                   where Δ = driverHeading − bearing(driver → pickup)
 *   score         = wDistance·distanceScore + wHeading·headingScore
 * </pre>
 *
 * A candidate with no known heading scores a neutral 0.5 on that term — we
 * neither reward nor punish missing telemetry.
 *
 * <h2>Determinism</h2>
 * Ties are broken by driver id so two replicas scoring the same snapshot agree
 * on the same winner ordering — important because they then contend for the
 * same lock in the same order, minimising wasted lock attempts.
 */
@Component
public class DispatchScorer {

    private final double wDistance;
    private final double wHeading;

    public DispatchScorer(DispatchProperties props) {
        double d = props.weight().distance();
        double h = props.weight().heading();
        double sum = d + h;
        if (sum <= 0) {
            throw new IllegalStateException("dispatch weights must be positive, got " + d + "/" + h);
        }
        // Normalise defensively so a misconfigured pair (not summing to 1.0)
        // still yields a well-formed [0,1] score rather than silently skewing.
        this.wDistance = d / sum;
        this.wHeading  = h / sum;
    }

    /**
     * Returns the candidates scored and sorted best-first. The caller iterates
     * this order, attempting the dispatch lock on each until one is acquired.
     */
    public List<DispatchCandidate> rank(List<DispatchCandidate> candidates,
                                        GeoPoint pickup,
                                        int searchRadiusMeters) {
        return candidates.stream()
                .map(c -> c.withScore(score(c, pickup, searchRadiusMeters)))
                .sorted(Comparator
                        .comparingDouble(DispatchCandidate::score).reversed()
                        .thenComparing(DispatchCandidate::driverId))
                .toList();
    }

    double score(DispatchCandidate c, GeoPoint pickup, int searchRadiusMeters) {
        return wDistance * distanceScore(c.distanceMeters(), searchRadiusMeters)
             + wHeading  * headingScore(c, pickup);
    }

    private static double distanceScore(double distanceMeters, int radiusMeters) {
        if (radiusMeters <= 0) return 0.0;
        return clamp01(1.0 - (distanceMeters / radiusMeters));
    }

    private static double headingScore(DispatchCandidate c, GeoPoint pickup) {
        if (c.headingDegrees() == null) {
            return 0.5;                       // neutral — unknown heading
        }
        double bearingToPickup = c.location().bearingTo(pickup);
        double deltaDeg = c.headingDegrees() - bearingToPickup;
        double cos = Math.cos(Math.toRadians(deltaDeg));
        return (1.0 + cos) / 2.0;             // 1.0 aligned, 0.0 opposite
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
