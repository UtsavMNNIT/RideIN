package com.rideflow.location.application.usecase;

import com.rideflow.location.domain.model.DriverAvailability;
import com.rideflow.location.infrastructure.messaging.kafka.dto.DriverAvailabilityChangedPayloadDto;
import com.rideflow.location.support.InMemoryDriverLocationRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyncDriverAvailabilityUseCaseTest {

    private InMemoryDriverLocationRepository repository;
    private SyncDriverAvailabilityUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDriverLocationRepository();
        useCase = new SyncDriverAvailabilityUseCase(repository, new SimpleMeterRegistry());
    }

    private static DriverAvailabilityChangedPayloadDto p(UUID id, String avail, Double lat, Double lng) {
        return new DriverAvailabilityChangedPayloadDto(id, avail, "STANDARD", lat, lng, Instant.now());
    }

    @Test
    void offline_removesDriver() {
        UUID id = UUID.randomUUID();
        useCase.handle(p(id, "OFFLINE", null, null));
        assertThat(repository.removed).containsExactly(id);
    }

    @Test
    void online_withLocation_upserts() {
        UUID id = UUID.randomUUID();
        useCase.handle(p(id, "ONLINE", 12.97, 77.59));
        assertThat(repository.upsertCount).isEqualTo(1);
        assertThat(repository.availabilityOf(id)).isEqualTo(DriverAvailability.ONLINE);
    }

    @Test
    void online_withoutLocation_isNoOp() {
        UUID id = UUID.randomUUID();
        useCase.handle(p(id, "ONLINE", null, null));
        assertThat(repository.upsertCount).isZero();
        assertThat(repository.removeCount).isZero();
    }

    @Test
    void onTrip_withLocation_upsertsAsBusy() {
        UUID id = UUID.randomUUID();
        useCase.handle(p(id, "ON_TRIP", 12.97, 77.59));
        assertThat(repository.upsertCount).isEqualTo(1);
        assertThat(repository.availabilityOf(id)).isEqualTo(DriverAvailability.ON_TRIP);
    }

    @Test
    void onTrip_withoutLocation_marksBusyInPlace() {
        UUID id = UUID.randomUUID();
        useCase.handle(p(id, "ON_TRIP", null, null));
        assertThat(repository.markedBusy).containsExactly(id);
        assertThat(repository.upsertCount).isZero();
    }

    @Test
    void unknownAvailability_throws() {
        assertThatThrownBy(() -> useCase.handle(p(UUID.randomUUID(), "PARTY", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown availability");
    }

    @Test
    void missingDriverId_throws() {
        assertThatThrownBy(() -> useCase.handle(p(null, "ONLINE", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("driverId missing");
    }
}
