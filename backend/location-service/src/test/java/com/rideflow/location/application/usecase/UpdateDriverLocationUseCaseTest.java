package com.rideflow.location.application.usecase;

import com.rideflow.location.domain.model.DriverAvailability;
import com.rideflow.location.infrastructure.messaging.kafka.dto.DriverLocationPayloadDto;
import com.rideflow.location.support.InMemoryDriverLocationRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateDriverLocationUseCaseTest {

    private InMemoryDriverLocationRepository repository;
    private UpdateDriverLocationUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDriverLocationRepository();
        useCase = new UpdateDriverLocationUseCase(repository, new SimpleMeterRegistry());
    }

    private static DriverLocationPayloadDto payload(UUID id, String avail, Instant at) {
        return new DriverLocationPayloadDto(id, 12.97, 77.59, 90.0, 12.0,
                "STANDARD", avail, at);
    }

    @Test
    void upsertsLocation_andMapsAvailability() {
        UUID id = UUID.randomUUID();
        useCase.handle(payload(id, "ON_TRIP", Instant.now()));

        assertThat(repository.upsertCount).isEqualTo(1);
        assertThat(repository.availabilityOf(id)).isEqualTo(DriverAvailability.ON_TRIP);
    }

    @Test
    void staleReplay_isDroppedByRepository() {
        UUID id = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-01-01T00:00:10Z");
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        useCase.handle(payload(id, "ONLINE", t1));
        useCase.handle(payload(id, "ON_TRIP", t0)); // older -> stale, no-op

        assertThat(repository.upsertCount).isEqualTo(2);
        // First write wins; availability unchanged.
        assertThat(repository.availabilityOf(id)).isEqualTo(DriverAvailability.ONLINE);
    }

    @Test
    void nullPayload_throws() {
        assertThatThrownBy(() -> useCase.handle(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payload is null");
    }

    @Test
    void missingDriverId_throws() {
        assertThatThrownBy(() -> useCase.handle(payload(null, "ONLINE", Instant.now())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("driverId missing");
    }

    @Test
    void unknownAvailability_throws() {
        assertThatThrownBy(() -> useCase.handle(payload(UUID.randomUUID(), "BOGUS", Instant.now())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown availability");
    }

    @Test
    void unknownVehicleType_throws() {
        DriverLocationPayloadDto p = new DriverLocationPayloadDto(UUID.randomUUID(), 12.0, 77.0,
                null, null, "ROCKET", "ONLINE", Instant.now());
        assertThatThrownBy(() -> useCase.handle(p))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown vehicleType");
    }
}
