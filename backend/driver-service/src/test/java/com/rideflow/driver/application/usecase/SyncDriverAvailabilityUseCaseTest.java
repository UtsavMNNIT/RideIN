package com.rideflow.driver.application.usecase;

import com.rideflow.driver.domain.model.DriverAvailability;
import com.rideflow.driver.support.DriverFixtures;
import com.rideflow.driver.support.InMemoryDriverRepository;
import com.rideflow.driver.support.InMemoryProcessedEventStore;
import com.rideflow.driver.support.RecordingDomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SyncDriverAvailabilityUseCaseTest {

    private static final String GROUP = "driver-service-ride-lifecycle";
    private static final UUID DRIVER = UUID.randomUUID();

    private InMemoryDriverRepository repo;
    private InMemoryProcessedEventStore processed;
    private RecordingDomainEventPublisher events;
    private SyncDriverAvailabilityUseCase useCase;

    @BeforeEach
    void setUp() {
        repo = new InMemoryDriverRepository();
        processed = new InMemoryProcessedEventStore();
        events = new RecordingDomainEventPublisher();
        useCase = new SyncDriverAvailabilityUseCase(repo, processed, events, GROUP);
    }

    // ------------------------------------------------------------------
    // onAccepted → ON_TRIP
    // ------------------------------------------------------------------

    @Test
    void onAccepted_flipsToOnTrip_marksProcessed_publishes() {
        repo.seed(DriverFixtures.online(DRIVER));
        UUID eventId = UUID.randomUUID();

        useCase.onAccepted(eventId, DRIVER);

        assertThat(repo.findById(DRIVER).orElseThrow().availability()).isEqualTo(DriverAvailability.ON_TRIP);
        assertThat(processed.isProcessed(eventId, GROUP)).isTrue();
        assertThat(events.availabilityChanges).hasSize(1);
        assertThat(events.availabilityChanges.get(0).availability()).isEqualTo("ON_TRIP");
    }

    // ------------------------------------------------------------------
    // onTripEnded → ONLINE
    // ------------------------------------------------------------------

    @Test
    void onTripEnded_flipsBackToOnline_publishes() {
        repo.seed(DriverFixtures.onTrip(DRIVER));
        UUID eventId = UUID.randomUUID();

        useCase.onTripEnded(eventId, DRIVER);

        assertThat(repo.findById(DRIVER).orElseThrow().availability()).isEqualTo(DriverAvailability.ONLINE);
        assertThat(events.availabilityChanges).hasSize(1);
        assertThat(events.availabilityChanges.get(0).availability()).isEqualTo("ONLINE");
    }

    // ------------------------------------------------------------------
    // idempotency — second delivery of the same eventId is a no-op
    // ------------------------------------------------------------------

    @Test
    void onAccepted_redeliverySameEventId_isNoOp() {
        repo.seed(DriverFixtures.online(DRIVER));
        UUID eventId = UUID.randomUUID();

        useCase.onAccepted(eventId, DRIVER);     // first delivery
        useCase.onAccepted(eventId, DRIVER);     // redelivery — must be skipped

        assertThat(repo.findById(DRIVER).orElseThrow().availability()).isEqualTo(DriverAvailability.ON_TRIP);
        // Only the first delivery published / processed once.
        assertThat(events.availabilityChanges).hasSize(1);
        assertThat(processed.size()).isEqualTo(1);
    }

    @Test
    void differentEventIds_areBothProcessed() {
        repo.seed(DriverFixtures.online(DRIVER));
        useCase.onAccepted(UUID.randomUUID(), DRIVER);
        useCase.onTripEnded(UUID.randomUUID(), DRIVER);
        assertThat(events.availabilityChanges).hasSize(2);
        assertThat(processed.size()).isEqualTo(2);
    }

    // ------------------------------------------------------------------
    // stray / unknown driver — marked processed, no publish, no throw
    // ------------------------------------------------------------------

    @Test
    void onAccepted_unknownDriver_markedProcessed_noPublish_noThrow() {
        UUID eventId = UUID.randomUUID();
        useCase.onAccepted(eventId, DRIVER); // driver not seeded

        assertThat(processed.isProcessed(eventId, GROUP)).isTrue();
        assertThat(events.availabilityChanges).isEmpty();
    }

    @Test
    void onTripEnded_toleratesDriverNotOnTrip() {
        repo.seed(DriverFixtures.online(DRIVER)); // already ONLINE, not ON_TRIP
        useCase.onTripEnded(UUID.randomUUID(), DRIVER);
        // endTrip() is a no-op, but availability stays ONLINE and we still publish.
        assertThat(repo.findById(DRIVER).orElseThrow().availability()).isEqualTo(DriverAvailability.ONLINE);
        assertThat(events.availabilityChanges).hasSize(1);
    }
}
