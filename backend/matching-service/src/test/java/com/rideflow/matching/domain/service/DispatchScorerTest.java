package com.rideflow.matching.domain.service;

import com.rideflow.matching.domain.model.DispatchCandidate;
import com.rideflow.matching.domain.model.GeoPoint;
import com.rideflow.matching.infrastructure.config.DispatchProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for the dispatch ranking algorithm. */
class DispatchScorerTest {

    private static final GeoPoint PICKUP = new GeoPoint(12.9716, 77.5946);
    private static final int RADIUS = 5000;

    private static DispatchProperties props(double wDistance, double wHeading) {
        return new DispatchProperties(
                10, 30_000L, 3, List.of(3000, 5000, 8000),
                new DispatchProperties.Weight(wDistance, wHeading));
    }

    private static DispatchScorer scorer() {
        return new DispatchScorer(props(0.70, 0.30));
    }

    private static DispatchCandidate candidate(UUID id, double distanceMeters, Double heading) {
        // location placed slightly south-west of pickup so bearingTo(pickup) is well-defined.
        return new DispatchCandidate(id, new GeoPoint(12.9700, 77.5900), distanceMeters, heading, null, null);
    }

    @Test
    void closerCandidateRanksFirst_whenHeadingsNeutral() {
        UUID near = new UUID(0, 1);
        UUID far = new UUID(0, 2);
        List<DispatchCandidate> ranked = scorer().rank(
                List.of(candidate(far, 4000, null), candidate(near, 500, null)),
                PICKUP, RADIUS);

        assertThat(ranked).extracting(DispatchCandidate::driverId)
                .containsExactly(near, far);
        assertThat(ranked.get(0).score()).isGreaterThan(ranked.get(1).score());
    }

    @Test
    void betterHeadingRanksFirst_atEqualDistance() {
        UUID aligned = new UUID(0, 1);
        UUID opposite = new UUID(0, 2);

        // Compute the bearing from the candidate location to the pickup so we can
        // build an aligned vs opposite heading deterministically.
        GeoPoint loc = new GeoPoint(12.9700, 77.5900);
        double bearingToPickup = loc.bearingTo(PICKUP);
        double oppositeHeading = (bearingToPickup + 180.0) % 360.0;

        DispatchCandidate alignedC = new DispatchCandidate(aligned, loc, 1000, bearingToPickup, null, null);
        DispatchCandidate oppositeC = new DispatchCandidate(opposite, loc, 1000, oppositeHeading, null, null);

        List<DispatchCandidate> ranked = scorer().rank(List.of(oppositeC, alignedC), PICKUP, RADIUS);

        assertThat(ranked.get(0).driverId()).isEqualTo(aligned);
        assertThat(ranked.get(1).driverId()).isEqualTo(opposite);
    }

    @Test
    void unknownHeadingScoresNeutral_betweenAlignedAndOpposite() {
        GeoPoint loc = new GeoPoint(12.9700, 77.5900);
        double bearingToPickup = loc.bearingTo(PICKUP);
        double oppositeHeading = (bearingToPickup + 180.0) % 360.0;

        DispatchScorer scorer = scorer();
        double alignedScore = scorer.score(
                new DispatchCandidate(new UUID(0, 1), loc, 1000, bearingToPickup, null, null), PICKUP, RADIUS);
        double neutralScore = scorer.score(
                new DispatchCandidate(new UUID(0, 2), loc, 1000, null, null, null), PICKUP, RADIUS);
        double oppositeScore = scorer.score(
                new DispatchCandidate(new UUID(0, 3), loc, 1000, oppositeHeading, null, null), PICKUP, RADIUS);

        assertThat(neutralScore).isLessThan(alignedScore).isGreaterThan(oppositeScore);
    }

    @Test
    void tiesBrokenByDriverId_forDeterminism() {
        // Identical distance + heading => identical score; lower driverId wins.
        UUID lo = new UUID(0, 1);
        UUID hi = new UUID(0, 2);
        DispatchCandidate a = candidate(hi, 1000, null);
        DispatchCandidate b = candidate(lo, 1000, null);

        List<DispatchCandidate> ranked = scorer().rank(List.of(a, b), PICKUP, RADIUS);
        assertThat(ranked).extracting(DispatchCandidate::driverId).containsExactly(lo, hi);
    }

    @Test
    void scoreIsWithinUnitInterval() {
        DispatchScorer scorer = scorer();
        double s = scorer.score(candidate(new UUID(0, 1), 2500, 45.0), PICKUP, RADIUS);
        assertThat(s).isBetween(0.0, 1.0);
    }

    @Test
    void distanceBeyondRadius_clampsToZeroDistanceTerm() {
        // distance > radius => distanceScore clamps to 0; with neutral heading
        // total score == wHeading * 0.5.
        DispatchScorer scorer = scorer();
        double s = scorer.score(candidate(new UUID(0, 1), 9999, null), PICKUP, RADIUS);
        assertThat(s).isCloseTo(0.30 * 0.5, org.assertj.core.api.Assertions.within(1e-9));
    }

    @Test
    void weightsAreNormalised_evenWhenNotSummingToOne() {
        // distance-only weighting (heading 0): closer must always win regardless of heading.
        DispatchScorer distanceOnly = new DispatchScorer(props(2.0, 0.0));
        UUID near = new UUID(0, 1);
        UUID far = new UUID(0, 2);
        List<DispatchCandidate> ranked = distanceOnly.rank(
                List.of(candidate(far, 4000, 0.0), candidate(near, 100, 180.0)),
                PICKUP, RADIUS);
        assertThat(ranked.get(0).driverId()).isEqualTo(near);
    }

    @Test
    void nonPositiveWeightSum_isRejected() {
        assertThatThrownBy(() -> new DispatchScorer(props(0.0, 0.0)))
                .isInstanceOf(IllegalStateException.class);
    }
}
