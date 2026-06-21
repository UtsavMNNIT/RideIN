package com.rideflow.driver.application.usecase;

import com.rideflow.driver.domain.exception.DriverNotFoundException;
import com.rideflow.driver.domain.exception.IllegalDriverStateException;
import com.rideflow.driver.domain.model.Driver;
import com.rideflow.driver.domain.model.DriverAvailability;
import com.rideflow.driver.support.DriverFixtures;
import com.rideflow.driver.support.InMemoryDriverRepository;
import com.rideflow.driver.support.RecordingDomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriverAvailabilityUseCaseTest {

    private static final UUID ID = UUID.randomUUID();

    private InMemoryDriverRepository repo;
    private RecordingDomainEventPublisher events;
    private DriverAvailabilityUseCase useCase;

    @BeforeEach
    void setUp() {
        repo = new InMemoryDriverRepository();
        events = new RecordingDomainEventPublisher();
        useCase = new DriverAvailabilityUseCase(repo, events);
    }

    @Test
    void goOnline_changesAvailability_andPublishesEvent() {
        repo.seed(DriverFixtures.offline(ID));

        Driver d = useCase.goOnline(ID);

        assertThat(d.availability()).isEqualTo(DriverAvailability.ONLINE);
        assertThat(repo.findById(ID).orElseThrow().availability()).isEqualTo(DriverAvailability.ONLINE);
        assertThat(events.availabilityChanges).hasSize(1);
        var ev = events.availabilityChanges.get(0);
        assertThat(ev.driverId()).isEqualTo(ID);
        assertThat(ev.availability()).isEqualTo("ONLINE");
        assertThat(ev.vehicleType()).isEqualTo("STANDARD");
    }

    @Test
    void goOffline_changesAvailability_andPublishesEvent() {
        repo.seed(DriverFixtures.online(ID));

        Driver d = useCase.goOffline(ID);

        assertThat(d.availability()).isEqualTo(DriverAvailability.OFFLINE);
        assertThat(events.availabilityChanges).hasSize(1);
        assertThat(events.availabilityChanges.get(0).availability()).isEqualTo("OFFLINE");
    }

    @Test
    void goOnline_carriesLastLocationWhenPresent() {
        repo.seed(DriverFixtures.online(ID)); // already online with a location
        useCase.goOnline(ID); // idempotent, still publishes
        var ev = events.availabilityChanges.get(0);
        assertThat(ev.lat()).isEqualTo(12.9);
        assertThat(ev.lng()).isEqualTo(77.5);
    }

    @Test
    void goOffline_whileOnTrip_throws_noEvent() {
        repo.seed(DriverFixtures.onTrip(ID));
        assertThatThrownBy(() -> useCase.goOffline(ID))
                .isInstanceOf(IllegalDriverStateException.class);
        assertThat(events.availabilityChanges).isEmpty();
    }

    @Test
    void goOnline_unknownDriver_throwsNotFound() {
        assertThatThrownBy(() -> useCase.goOnline(ID))
                .isInstanceOf(DriverNotFoundException.class);
        assertThat(events.availabilityChanges).isEmpty();
    }
}
