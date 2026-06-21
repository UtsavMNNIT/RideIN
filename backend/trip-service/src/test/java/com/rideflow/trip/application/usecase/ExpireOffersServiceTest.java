package com.rideflow.trip.application.usecase;

import com.rideflow.trip.domain.model.GeoPoint;
import com.rideflow.trip.domain.model.Trip;
import com.rideflow.trip.domain.model.TripStatus;
import com.rideflow.trip.domain.model.VehicleType;
import com.rideflow.trip.support.InMemoryTripRepository;
import com.rideflow.trip.support.RecordingTripEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExpireOffersServiceTest {

    // The sweeper queries with the real wall clock (Instant.now()), so offers
    // must be seeded relative to it: "expired" = clearly in the past.
    private static final Instant NOW = Instant.now();

    private InMemoryTripRepository repo;
    private RecordingTripEventPublisher events;
    private TripActionService actions;
    private ExpireOffersService sweeper;

    @BeforeEach
    void setUp() {
        repo = new InMemoryTripRepository();
        events = new RecordingTripEventPublisher();
        actions = new TripActionService(repo, events);
        sweeper = new ExpireOffersService(repo, actions);
    }

    private Trip offer(UUID id, Instant expiresAt) {
        return Trip.offer(id, UUID.randomUUID(), UUID.randomUUID(),
                new GeoPoint(12.9, 77.5), new GeoPoint(13.0, 77.6),
                VehicleType.STANDARD, 0.9, NOW.minusSeconds(60), expiresAt);
    }

    @Test
    void sweep_expiresLapsedOffers_andPublishesRejected() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        repo.seed(offer(a, NOW.minusSeconds(10)));   // expired
        repo.seed(offer(b, NOW.minusSeconds(1)));    // expired

        sweeper.sweep();

        assertThat(repo.findById(a).orElseThrow().status()).isEqualTo(TripStatus.EXPIRED);
        assertThat(repo.findById(b).orElseThrow().status()).isEqualTo(TripStatus.EXPIRED);
        assertThat(events.rejected).hasSize(2);
        assertThat(events.rejected).allSatisfy(e ->
                assertThat(e.reason()).isEqualTo(Trip.REASON_EXPIRED));
    }

    @Test
    void sweep_leavesFreshOffersUntouched() {
        UUID fresh = UUID.randomUUID();
        repo.seed(offer(fresh, NOW.plusSeconds(60))); // not yet expired

        sweeper.sweep();

        assertThat(repo.findById(fresh).orElseThrow().status()).isEqualTo(TripStatus.OFFERED);
        assertThat(events.total()).isZero();
    }

    @Test
    void sweep_emptyBatch_isNoOp() {
        sweeper.sweep();
        assertThat(events.total()).isZero();
    }

    @Test
    void sweep_skipsOptimisticLockLoser_andContinues() {
        // 'racing' loses the version check (driver accepted first); 'ok' still expires.
        UUID racing = UUID.randomUUID();
        UUID ok = UUID.randomUUID();
        repo.seed(offer(racing, NOW.minusSeconds(10)));
        repo.seed(offer(ok, NOW.minusSeconds(10)));

        // Wrap actions so expire(racing) throws an optimistic-lock failure,
        // mirroring the accept-vs-expire race documented on the sweeper.
        TripActionService throwingActions = new TripActionService(repo, events) {
            @Override
            public void expire(UUID rideId) {
                if (rideId.equals(racing)) {
                    throw new OptimisticLockingFailureException("driver responded first");
                }
                super.expire(rideId);
            }
        };
        ExpireOffersService s = new ExpireOffersService(repo, throwingActions);

        s.sweep(); // must not propagate the exception

        assertThat(repo.findById(ok).orElseThrow().status()).isEqualTo(TripStatus.EXPIRED);
        assertThat(repo.findById(racing).orElseThrow().status()).isEqualTo(TripStatus.OFFERED);
        assertThat(events.rejected).hasSize(1);
    }

    @Test
    void sweep_swallowsUnexpectedError_andContinues() {
        UUID bad = UUID.randomUUID();
        UUID good = UUID.randomUUID();
        repo.seed(offer(bad, NOW.minusSeconds(10)));
        repo.seed(offer(good, NOW.minusSeconds(10)));

        TripActionService throwingActions = new TripActionService(repo, events) {
            @Override
            public void expire(UUID rideId) {
                if (rideId.equals(bad)) {
                    throw new RuntimeException("boom");
                }
                super.expire(rideId);
            }
        };
        ExpireOffersService s = new ExpireOffersService(repo, throwingActions);

        s.sweep();

        assertThat(repo.findById(good).orElseThrow().status()).isEqualTo(TripStatus.EXPIRED);
        assertThat(events.rejected).hasSize(1);
    }
}
