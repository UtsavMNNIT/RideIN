package com.rideflow.rider.application.usecase;

import com.rideflow.rider.domain.model.GeoPoint;
import com.rideflow.rider.domain.model.Ride;
import com.rideflow.rider.domain.model.RideStatus;
import com.rideflow.rider.domain.model.VehicleType;
import com.rideflow.rider.support.InMemoryProcessedEventStore;
import com.rideflow.rider.support.InMemoryRideRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectRideUseCaseTest {

    private static final String GROUP = "rider-projection";

    private InMemoryRideRepository rides;
    private InMemoryProcessedEventStore processed;
    private ProjectRideUseCase useCase;

    @BeforeEach
    void setUp() {
        rides = new InMemoryRideRepository();
        processed = new InMemoryProcessedEventStore();
        useCase = new ProjectRideUseCase(rides, processed, GROUP);
    }

    private Ride seedRequested() {
        Ride ride = Ride.request(UUID.randomUUID(),
                new GeoPoint(12.97, 77.59), new GeoPoint(12.93, 77.62), VehicleType.STANDARD);
        return rides.save(ride);
    }

    @Test
    void onAssigned_updatesRow() {
        Ride ride = seedRequested();
        UUID driver = UUID.randomUUID();

        useCase.onAssigned(UUID.randomUUID(), ride.id(), driver, 0.91);

        Ride updated = rides.get(ride.id());
        assertThat(updated.status()).isEqualTo(RideStatus.ASSIGNED);
        assertThat(updated.assignedDriverId()).isEqualTo(driver);
        assertThat(updated.matchScore()).isEqualTo(0.91);
    }

    @Test
    void onDispatchFailed_setsNoDriversFound() {
        Ride ride = seedRequested();
        useCase.onDispatchFailed(UUID.randomUUID(), ride.id());
        assertThat(rides.get(ride.id()).status()).isEqualTo(RideStatus.NO_DRIVERS_FOUND);
    }

    @Test
    void onStarted_setsStarted() {
        Ride ride = seedRequested();
        useCase.onAssigned(UUID.randomUUID(), ride.id(), UUID.randomUUID(), 0.5);
        useCase.onStarted(UUID.randomUUID(), ride.id());
        assertThat(rides.get(ride.id()).status()).isEqualTo(RideStatus.STARTED);
    }

    @Test
    void onCompleted_setsCompletedWithMetrics() {
        Ride ride = seedRequested();
        useCase.onCompleted(UUID.randomUUID(), ride.id(), 5000, 1200);
        Ride updated = rides.get(ride.id());
        assertThat(updated.status()).isEqualTo(RideStatus.COMPLETED);
        assertThat(updated.finalDistanceMeters()).isEqualTo(5000);
        assertThat(updated.finalDurationSeconds()).isEqualTo(1200);
    }

    @Test
    void onCancelled_setsCancelled() {
        Ride ride = seedRequested();
        useCase.onCancelled(UUID.randomUUID(), ride.id());
        assertThat(rides.get(ride.id()).status()).isEqualTo(RideStatus.CANCELLED);
    }

    @Test
    void onFareQuoted_attachesFare() {
        Ride ride = seedRequested();
        useCase.onFareQuoted(UUID.randomUUID(), ride.id(), new BigDecimal("412.00"), "INR");
        Ride updated = rides.get(ride.id());
        assertThat(updated.fareTotal()).isEqualByComparingTo("412.00");
        assertThat(updated.currency()).isEqualTo("INR");
    }

    @Test
    void duplicateEventId_isIdempotent_doesNotReapply() {
        Ride ride = seedRequested();
        UUID eventId = UUID.randomUUID();

        useCase.onAssigned(eventId, ride.id(), UUID.randomUUID(), 0.5);
        int savesAfterFirst = rides.saveCount;

        // Replay same eventId — should be skipped entirely.
        useCase.onAssigned(eventId, ride.id(), UUID.randomUUID(), 0.99);

        assertThat(rides.saveCount).isEqualTo(savesAfterFirst);
        assertThat(rides.get(ride.id()).matchScore()).isEqualTo(0.5);
    }

    @Test
    void unknownRide_isMarkedProcessedAndNotSaved() {
        UUID eventId = UUID.randomUUID();
        UUID unknownRide = UUID.randomUUID();

        useCase.onStarted(eventId, unknownRide);

        assertThat(rides.saveCount).isZero();
        assertThat(processed.isProcessed(eventId, GROUP)).isTrue();
    }

    @Test
    void outOfOrderEvents_areAbsorbedMonotonically() {
        Ride ride = seedRequested();
        // Completed arrives before started/assigned.
        useCase.onCompleted(UUID.randomUUID(), ride.id(), 100, 60);
        assertThat(rides.get(ride.id()).status()).isEqualTo(RideStatus.COMPLETED);

        // A late "started" must not move it backward.
        useCase.onStarted(UUID.randomUUID(), ride.id());
        assertThat(rides.get(ride.id()).status()).isEqualTo(RideStatus.COMPLETED);
    }
}
