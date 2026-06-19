package com.rideflow.matching.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One pass of the radius-expansion loop. Persisted to {@code dispatch_attempts}
 * for forensics (why did this ride take 3 attempts, what radius worked).
 */
public record DispatchAttempt(
        int             attemptNo,
        int             radiusMeters,
        int             candidatesFound,
        UUID            selectedDriverId,    // nullable if no candidates
        Double          selectedScore,       // nullable
        DispatchOutcome outcome,
        int             durationMs,
        Instant         attemptedAt
) {

    public static DispatchAttempt noCandidates(int attemptNo, int radiusMeters, int durationMs) {
        return new DispatchAttempt(attemptNo, radiusMeters, 0, null, null,
                DispatchOutcome.NO_CANDIDATES, durationMs, Instant.now());
    }

    public static DispatchAttempt success(int attemptNo, int radiusMeters, int found,
                                          DispatchCandidate winner, int durationMs) {
        return new DispatchAttempt(attemptNo, radiusMeters, found,
                winner.driverId(), winner.score(),
                DispatchOutcome.SUCCESS, durationMs, Instant.now());
    }
}
