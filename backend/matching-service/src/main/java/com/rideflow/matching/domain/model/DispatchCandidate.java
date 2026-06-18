package com.rideflow.matching.domain.model;

import java.util.UUID;

/**
 * A driver under consideration during dispatch.
 *
 * Populated by the {@link com.rideflow.matching.domain.repository.DispatchCandidateProvider}
 * — distance comes from Redis {@code WITHDIST}; heading/speed come from the
 * driver-meta hash via a pipelined {@code HMGET}.
 *
 * Score is filled in by the scorer in a later stage; absent before scoring.
 */
public record DispatchCandidate(
        UUID      driverId,
        GeoPoint  location,
        double    distanceMeters,
        Double    headingDegrees,    // nullable — neutral score if absent
        Double    speedMps,
        Double    score              // null until scored
) {

    public DispatchCandidate withScore(double s) {
        return new DispatchCandidate(driverId, location, distanceMeters,
                headingDegrees, speedMps, s);
    }
}
