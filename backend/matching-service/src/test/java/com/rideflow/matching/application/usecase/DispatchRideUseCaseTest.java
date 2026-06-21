package com.rideflow.matching.application.usecase;

import com.rideflow.matching.application.port.out.DispatchEventPublisher;
import com.rideflow.matching.application.port.out.DriverLock;
import com.rideflow.matching.application.port.out.DriverLockService;
import com.rideflow.matching.application.port.out.ProcessedEventStore;
import com.rideflow.matching.application.port.out.RideRepository;
import com.rideflow.matching.domain.event.RideAssigned;
import com.rideflow.matching.domain.event.RideDispatchFailed;
import com.rideflow.matching.domain.model.DispatchAttempt;
import com.rideflow.matching.domain.model.DispatchCandidate;
import com.rideflow.matching.domain.model.GeoPoint;
import com.rideflow.matching.domain.model.Ride;
import com.rideflow.matching.domain.model.RideStatus;
import com.rideflow.matching.domain.model.VehicleType;
import com.rideflow.matching.domain.repository.DispatchCandidateProvider;
import com.rideflow.matching.domain.service.DispatchScorer;
import com.rideflow.matching.infrastructure.config.DispatchProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the dispatch orchestrator. Uses a real {@link DispatchScorer},
 * a real {@link DispatchCommitService} wired to hand-rolled fakes, a real
 * {@link SimpleMeterRegistry}, and hand-rolled fakes for every output port.
 */
class DispatchRideUseCaseTest {

    private static final String GROUP = "matching-service";
    private static final GeoPoint PICKUP = new GeoPoint(12.9716, 77.5946);
    private static final GeoPoint DROPOFF = new GeoPoint(13.00, 77.62);
    private static final Instant T0 = Instant.parse("2026-06-20T10:00:00Z");

    // ---- fakes ------------------------------------------------------------

    /** Fake candidate provider: returns configured candidates per radius, honouring exclusions. */
    private static final class FakeCandidateProvider implements DispatchCandidateProvider {
        final Map<Integer, List<DispatchCandidate>> byRadius = new HashMap<>();
        Set<UUID> lastExcluded = Set.of();

        void put(int radius, DispatchCandidate... cands) { byRadius.put(radius, List.of(cands)); }

        @Override
        public List<DispatchCandidate> findCandidates(GeoPoint pickup, VehicleType vehicleType,
                                                      int radiusMeters, int limit, Set<UUID> excludedDriverIds) {
            this.lastExcluded = excludedDriverIds;
            List<DispatchCandidate> all = byRadius.getOrDefault(radiusMeters, List.of());
            List<DispatchCandidate> out = new ArrayList<>();
            for (DispatchCandidate c : all) {
                if (!excludedDriverIds.contains(c.driverId())) out.add(c);
            }
            return out;
        }
    }

    /** Fake lock: acquires for everyone except a configured set of "held" driver ids. */
    private static final class FakeLockService implements DriverLockService {
        final Set<UUID> heldElsewhere = new HashSet<>();
        final List<UUID> acquireAttempts = new ArrayList<>();

        @Override
        public DriverLock tryAcquire(UUID driverId) {
            acquireAttempts.add(driverId);
            boolean acquired = !heldElsewhere.contains(driverId);
            return new FakeLock(driverId, acquired);
        }
    }

    private static final class FakeLock implements DriverLock {
        private final UUID driverId;
        private final boolean acquired;
        FakeLock(UUID driverId, boolean acquired) { this.driverId = driverId; this.acquired = acquired; }
        @Override public UUID driverId() { return driverId; }
        @Override public boolean isAcquired() { return acquired; }
        @Override public void close() { }
    }

    private static final class FakeProcessedEvents implements ProcessedEventStore {
        final Set<String> processed = new HashSet<>();
        @Override public boolean isProcessed(UUID eventId, String consumerGroup) {
            return processed.contains(eventId + "|" + consumerGroup);
        }
        @Override public void markProcessed(UUID eventId, String consumerGroup) {
            processed.add(eventId + "|" + consumerGroup);
        }
    }

    private static final class FakeRideRepository implements RideRepository {
        final Map<UUID, Ride> rides = new HashMap<>();
        final List<Ride> savedOutcomes = new ArrayList<>();
        final List<Ride> updatedOutcomes = new ArrayList<>();

        @Override public boolean existsById(UUID rideId) { return rides.containsKey(rideId); }
        @Override public void saveOutcome(Ride ride, List<DispatchAttempt> attempts) {
            savedOutcomes.add(ride);
            rides.put(ride.id(), ride);
        }
        @Override public void updateOutcome(Ride ride, List<DispatchAttempt> attempts) {
            updatedOutcomes.add(ride);
            rides.put(ride.id(), ride);
        }
        @Override public Optional<Ride> findById(UUID rideId) { return Optional.ofNullable(rides.get(rideId)); }
    }

    private static final class FakeEventPublisher implements DispatchEventPublisher {
        final List<RideAssigned> assigned = new ArrayList<>();
        final List<RideDispatchFailed> failed = new ArrayList<>();
        @Override public void publishRideAssigned(RideAssigned event) { assigned.add(event); }
        @Override public void publishRideDispatchFailed(RideDispatchFailed event) { failed.add(event); }
    }

    // ---- fixtures ---------------------------------------------------------

    private FakeCandidateProvider candidates;
    private FakeLockService locks;
    private FakeProcessedEvents processed;
    private FakeRideRepository rides;
    private FakeEventPublisher publisher;
    private DispatchCommitService commitService;
    private DispatchProperties props;
    private SimpleMeterRegistry registry;
    private DispatchRideUseCase useCase;

    private static DispatchProperties props() {
        return new DispatchProperties(
                10, 30_000L, 2, List.of(3000, 5000, 8000),
                new DispatchProperties.Weight(0.70, 0.30));
    }

    private static DispatchCandidate candidate(UUID id, double distanceMeters) {
        return new DispatchCandidate(id, new GeoPoint(12.9700, 77.5900), distanceMeters, null, null, null);
    }

    @BeforeEach
    void setUp() {
        candidates = new FakeCandidateProvider();
        locks = new FakeLockService();
        processed = new FakeProcessedEvents();
        rides = new FakeRideRepository();
        publisher = new FakeEventPublisher();
        commitService = new DispatchCommitService(rides, publisher, processed);
        props = props();
        registry = new SimpleMeterRegistry();
        useCase = new DispatchRideUseCase(candidates, new DispatchScorer(props), locks,
                commitService, processed, rides, props, registry, GROUP);
    }

    private DispatchRideUseCase.DispatchCommand command(UUID rideId, UUID eventId) {
        return new DispatchRideUseCase.DispatchCommand(
                eventId, rideId, UUID.randomUUID(), PICKUP, DROPOFF, VehicleType.STANDARD, T0);
    }

    // ---- 1. happy path ----------------------------------------------------

    @Test
    void dispatch_matchesBestCandidate() {
        UUID rideId = UUID.randomUUID();
        UUID near = new UUID(0, 1);
        UUID far = new UUID(0, 2);
        candidates.put(3000, candidate(far, 2500), candidate(near, 300));

        useCase.dispatch(command(rideId, UUID.randomUUID()));

        assertThat(publisher.assigned).hasSize(1);
        RideAssigned ev = publisher.assigned.get(0);
        assertThat(ev.driverId()).isEqualTo(near);   // closer driver wins
        assertThat(ev.rideId()).isEqualTo(rideId);

        Ride persisted = rides.rides.get(rideId);
        assertThat(persisted.status()).isEqualTo(RideStatus.ASSIGNED);
        assertThat(persisted.assignedDriverId()).isEqualTo(near);
        assertThat(registry.counter("dispatch.outcome", "result", "matched").count()).isEqualTo(1.0);
    }

    @Test
    void dispatch_expandsRadius_whenCloserRingEmpty() {
        UUID rideId = UUID.randomUUID();
        UUID driver = new UUID(0, 7);
        // first ring empty, second ring has a candidate
        candidates.put(5000, candidate(driver, 4000));

        useCase.dispatch(command(rideId, UUID.randomUUID()));

        assertThat(publisher.assigned).hasSize(1);
        assertThat(publisher.assigned.get(0).driverId()).isEqualTo(driver);
    }

    @Test
    void dispatch_skipsContendedDriver_locksNextBest() {
        UUID rideId = UUID.randomUUID();
        UUID best = new UUID(0, 1);
        UUID second = new UUID(0, 2);
        candidates.put(3000, candidate(best, 200), candidate(second, 1000));
        locks.heldElsewhere.add(best);   // best is locked elsewhere

        useCase.dispatch(command(rideId, UUID.randomUUID()));

        assertThat(publisher.assigned).hasSize(1);
        assertThat(publisher.assigned.get(0).driverId()).isEqualTo(second);
        assertThat(registry.counter("dispatch.candidate", "result", "contended").count()).isEqualTo(1.0);
    }

    // ---- 2. no candidates -> failure --------------------------------------

    @Test
    void dispatch_noCandidatesInAnyRing_commitsFailure() {
        UUID rideId = UUID.randomUUID();
        useCase.dispatch(command(rideId, UUID.randomUUID()));

        assertThat(publisher.assigned).isEmpty();
        assertThat(publisher.failed).hasSize(1);
        RideDispatchFailed ev = publisher.failed.get(0);
        assertThat(ev.rideId()).isEqualTo(rideId);
        assertThat(ev.reason()).isEqualTo("NO_DRIVERS_IN_RANGE");

        Ride persisted = rides.rides.get(rideId);
        assertThat(persisted.status()).isEqualTo(RideStatus.DISPATCH_FAILED);
        assertThat(registry.counter("dispatch.outcome", "result", "failed").count()).isEqualTo(1.0);
    }

    // ---- 3. idempotency ---------------------------------------------------

    @Test
    void dispatch_skipsWhenEventAlreadyProcessed() {
        UUID rideId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        processed.markProcessed(eventId, GROUP);
        candidates.put(3000, candidate(new UUID(0, 1), 200));

        useCase.dispatch(command(rideId, eventId));

        assertThat(publisher.assigned).isEmpty();
        assertThat(publisher.failed).isEmpty();
        assertThat(registry.counter("dispatch.outcome", "result", "duplicate").count()).isEqualTo(1.0);
    }

    @Test
    void dispatch_skipsWhenRideAlreadyExists() {
        UUID rideId = UUID.randomUUID();
        // pre-seed a ride row => existsById true
        rides.rides.put(rideId, Ride.requested(rideId, UUID.randomUUID(), PICKUP, DROPOFF, VehicleType.STANDARD, T0));
        candidates.put(3000, candidate(new UUID(0, 1), 200));

        useCase.dispatch(command(rideId, UUID.randomUUID()));

        assertThat(publisher.assigned).isEmpty();
        assertThat(registry.counter("dispatch.outcome", "result", "duplicate").count()).isEqualTo(1.0);
    }

    // ---- 4. redispatch ----------------------------------------------------

    private Ride seedAssignedRide(UUID rideId, UUID assignedDriver, int redispatchCount) {
        Ride ride = new Ride(rideId, UUID.randomUUID(), PICKUP, DROPOFF, VehicleType.STANDARD,
                RideStatus.ASSIGNED, assignedDriver, 0.9, null, T0, T0.plusSeconds(1), null, redispatchCount);
        rides.rides.put(rideId, ride);
        return ride;
    }

    @Test
    void redispatch_excludesRejectingDriver_andReassigns() {
        UUID rideId = UUID.randomUUID();
        UUID rejecting = new UUID(0, 1);
        UUID replacement = new UUID(0, 2);
        seedAssignedRide(rideId, rejecting, 0);

        // The rejecting driver is also "closest" but must be excluded.
        candidates.put(3000, candidate(rejecting, 100), candidate(replacement, 800));

        useCase.redispatch(UUID.randomUUID(), rideId, rejecting);

        assertThat(candidates.lastExcluded).contains(rejecting);
        assertThat(publisher.assigned).hasSize(1);
        assertThat(publisher.assigned.get(0).driverId()).isEqualTo(replacement);

        Ride persisted = rides.rides.get(rideId);
        assertThat(persisted.status()).isEqualTo(RideStatus.ASSIGNED);
        assertThat(persisted.assignedDriverId()).isEqualTo(replacement);
        assertThat(persisted.redispatchCount()).isEqualTo(1);   // incremented
        assertThat(rides.updatedOutcomes).hasSize(1);           // UPDATE not INSERT
        assertThat(registry.counter("redispatch.outcome", "result", "matched").count()).isEqualTo(1.0);
    }

    @Test
    void redispatch_noReplacement_commitsReassignmentFailure() {
        UUID rideId = UUID.randomUUID();
        UUID rejecting = new UUID(0, 1);
        seedAssignedRide(rideId, rejecting, 0);
        // only the rejecting driver around => excluded => no candidates
        candidates.put(3000, candidate(rejecting, 100));

        useCase.redispatch(UUID.randomUUID(), rideId, rejecting);

        assertThat(publisher.assigned).isEmpty();
        assertThat(publisher.failed).hasSize(1);
        assertThat(rides.rides.get(rideId).status()).isEqualTo(RideStatus.DISPATCH_FAILED);
        assertThat(registry.counter("redispatch.outcome", "result", "failed").count()).isEqualTo(1.0);
    }

    @Test
    void redispatch_whenMaxExceeded_commitsFailureWithoutSearching() {
        UUID rideId = UUID.randomUUID();
        UUID rejecting = new UUID(0, 1);
        // maxRedispatches is 2; seed at the bound.
        seedAssignedRide(rideId, rejecting, 2);
        candidates.put(3000, candidate(new UUID(0, 5), 100)); // would match if searched

        useCase.redispatch(UUID.randomUUID(), rideId, rejecting);

        assertThat(publisher.assigned).isEmpty();
        assertThat(publisher.failed).hasSize(1);
        assertThat(publisher.failed.get(0).reason()).isEqualTo("MAX_REDISPATCH_EXCEEDED");
        assertThat(rides.rides.get(rideId).status()).isEqualTo(RideStatus.DISPATCH_FAILED);
        assertThat(registry.counter("redispatch.outcome", "result", "exhausted").count()).isEqualTo(1.0);
    }

    @Test
    void redispatch_skipsWhenEventAlreadyProcessed() {
        UUID rideId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        seedAssignedRide(rideId, new UUID(0, 1), 0);
        processed.markProcessed(eventId, GROUP);

        useCase.redispatch(eventId, rideId, new UUID(0, 1));

        assertThat(publisher.assigned).isEmpty();
        assertThat(publisher.failed).isEmpty();
        assertThat(registry.counter("redispatch.outcome", "result", "duplicate").count()).isEqualTo(1.0);
    }

    @Test
    void redispatch_whenRideMissingOrNotRedispatchable_acksAndSkips() {
        UUID eventId = UUID.randomUUID();
        // ride not present in repo
        useCase.redispatch(eventId, UUID.randomUUID(), new UUID(0, 1));

        assertThat(publisher.assigned).isEmpty();
        assertThat(publisher.failed).isEmpty();
        assertThat(processed.isProcessed(eventId, GROUP)).isTrue(); // recorded so offset advances
        assertThat(registry.counter("redispatch.outcome", "result", "skipped").count()).isEqualTo(1.0);
    }
}
