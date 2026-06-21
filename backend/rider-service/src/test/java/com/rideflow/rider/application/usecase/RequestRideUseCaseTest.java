package com.rideflow.rider.application.usecase;

import com.rideflow.rider.application.usecase.RequestRideUseCase.RequestCommand;
import com.rideflow.rider.domain.event.RideRequested;
import com.rideflow.rider.domain.exception.RiderNotFoundException;
import com.rideflow.rider.domain.model.Ride;
import com.rideflow.rider.domain.model.RideStatus;
import com.rideflow.rider.domain.model.Rider;
import com.rideflow.rider.domain.model.VehicleType;
import com.rideflow.rider.support.FakeRideEventPublisher;
import com.rideflow.rider.support.InMemoryRideRepository;
import com.rideflow.rider.support.InMemoryRiderRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestRideUseCaseTest {

    private InMemoryRiderRepository riders;
    private InMemoryRideRepository rides;
    private FakeRideEventPublisher publisher;
    private RequestRideUseCase useCase;

    @BeforeEach
    void setUp() {
        riders = new InMemoryRiderRepository();
        rides = new InMemoryRideRepository();
        publisher = new FakeRideEventPublisher();
        useCase = new RequestRideUseCase(riders, rides, publisher);
    }

    private Rider seedRider() {
        Instant now = Instant.now();
        Rider rider = new Rider(UUID.randomUUID(), "a@b.com", "123", "Ann",
                "hash", now, now);
        return riders.seed(rider);
    }

    @Test
    void happyPath_persistsRideAndEnqueuesEvent() {
        Rider rider = seedRider();
        RequestCommand cmd = new RequestCommand(rider.id(),
                12.97, 77.59, 12.93, 77.62, VehicleType.PREMIUM);

        Ride saved = useCase.request(cmd);

        assertThat(saved.status()).isEqualTo(RideStatus.REQUESTED);
        assertThat(saved.riderId()).isEqualTo(rider.id());
        assertThat(saved.vehicleType()).isEqualTo(VehicleType.PREMIUM);
        // persisted
        assertThat(rides.get(saved.id())).isEqualTo(saved);
        assertThat(rides.saveCount).isEqualTo(1);
        // event enqueued with matching projection
        assertThat(publisher.published).hasSize(1);
        RideRequested ev = publisher.published.get(0);
        assertThat(ev.rideId()).isEqualTo(saved.id());
        assertThat(ev.riderId()).isEqualTo(rider.id());
        assertThat(ev.pickupLat()).isEqualTo(12.97);
        assertThat(ev.pickupLng()).isEqualTo(77.59);
        assertThat(ev.dropoffLat()).isEqualTo(12.93);
        assertThat(ev.dropoffLng()).isEqualTo(77.62);
        assertThat(ev.vehicleType()).isEqualTo("PREMIUM");
        assertThat(ev.requestedAt()).isEqualTo(saved.requestedAt());
    }

    @Test
    void unknownRider_throwsAndDoesNotPersistOrPublish() {
        RequestCommand cmd = new RequestCommand(UUID.randomUUID(),
                12.97, 77.59, 12.93, 77.62, VehicleType.STANDARD);

        assertThatThrownBy(() -> useCase.request(cmd))
                .isInstanceOf(RiderNotFoundException.class);

        assertThat(rides.saveCount).isZero();
        assertThat(publisher.published).isEmpty();
    }

    @Test
    void invalidPickupCoordinates_throwAndNothingIsPublished() {
        Rider rider = seedRider();
        RequestCommand cmd = new RequestCommand(rider.id(),
                999.0, 77.59, 12.93, 77.62, VehicleType.STANDARD);

        assertThatThrownBy(() -> useCase.request(cmd))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(rides.saveCount).isZero();
        assertThat(publisher.published).isEmpty();
    }
}
