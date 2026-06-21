package com.rideflow.location.application.usecase;

import com.rideflow.location.domain.model.NearbyDriver;
import com.rideflow.location.domain.model.NearbyQuery;
import com.rideflow.location.domain.model.VehicleType;
import com.rideflow.location.support.InMemoryDriverLocationRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FindNearbyDriversUseCaseTest {

    private InMemoryDriverLocationRepository repository;
    private FindNearbyDriversUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDriverLocationRepository();
        useCase = new FindNearbyDriversUseCase(repository, new SimpleMeterRegistry());
    }

    @Test
    void returnsCandidates_nearestFirst() {
        UUID far = UUID.randomUUID();
        UUID near = UUID.randomUUID();
        repository.seedCandidate(new NearbyDriver(far, 12.0, 77.0, 1500.0, VehicleType.STANDARD));
        repository.seedCandidate(new NearbyDriver(near, 12.0, 77.0, 300.0, VehicleType.STANDARD));

        List<NearbyDriver> result = useCase.handle(
                new NearbyQuery(12.0, 77.0, 5000, VehicleType.STANDARD, 10));

        assertThat(result).extracting(NearbyDriver::driverId).containsExactly(near, far);
    }

    @Test
    void filtersByVehicleType() {
        repository.seedCandidate(new NearbyDriver(UUID.randomUUID(), 12.0, 77.0, 100.0, VehicleType.STANDARD));
        repository.seedCandidate(new NearbyDriver(UUID.randomUUID(), 12.0, 77.0, 100.0, VehicleType.XL));

        List<NearbyDriver> result = useCase.handle(
                new NearbyQuery(12.0, 77.0, 5000, VehicleType.XL, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).vehicleType()).isEqualTo(VehicleType.XL);
    }

    @Test
    void respectsRadius() {
        repository.seedCandidate(new NearbyDriver(UUID.randomUUID(), 12.0, 77.0, 6000.0, VehicleType.STANDARD));

        List<NearbyDriver> result = useCase.handle(
                new NearbyQuery(12.0, 77.0, 5000, VehicleType.STANDARD, 10));

        assertThat(result).isEmpty();
    }

    @Test
    void respectsLimit() {
        for (int i = 0; i < 5; i++) {
            repository.seedCandidate(new NearbyDriver(UUID.randomUUID(), 12.0, 77.0, 100.0 + i, VehicleType.STANDARD));
        }
        List<NearbyDriver> result = useCase.handle(
                new NearbyQuery(12.0, 77.0, 5000, VehicleType.STANDARD, 2));
        assertThat(result).hasSize(2);
    }

    @Test
    void emptyWhenNoCandidates() {
        assertThat(useCase.handle(new NearbyQuery(0.0, 0.0, 1000, VehicleType.STANDARD, 5))).isEmpty();
    }
}
