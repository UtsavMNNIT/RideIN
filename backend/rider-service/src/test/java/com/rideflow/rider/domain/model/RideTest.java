package com.rideflow.rider.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideTest {

    private static final GeoPoint PICKUP  = new GeoPoint(12.97, 77.59);
    private static final GeoPoint DROPOFF = new GeoPoint(12.93, 77.62);

    private static Ride newRequest() {
        return Ride.request(UUID.randomUUID(), PICKUP, DROPOFF, VehicleType.STANDARD);
    }

    @Test
    void request_factory_initialisesRequestedState() {
        UUID riderId = UUID.randomUUID();
        Ride ride = Ride.request(riderId, PICKUP, DROPOFF, VehicleType.XL);

        assertThat(ride.id()).isNotNull();
        assertThat(ride.riderId()).isEqualTo(riderId);
        assertThat(ride.pickup()).isEqualTo(PICKUP);
        assertThat(ride.dropoff()).isEqualTo(DROPOFF);
        assertThat(ride.vehicleType()).isEqualTo(VehicleType.XL);
        assertThat(ride.status()).isEqualTo(RideStatus.REQUESTED);
        assertThat(ride.assignedDriverId()).isNull();
        assertThat(ride.fareTotal()).isNull();
        assertThat(ride.requestedAt()).isNotNull();
        assertThat(ride.createdAt()).isEqualTo(ride.requestedAt());
        assertThat(ride.updatedAt()).isEqualTo(ride.requestedAt());
    }

    @Test
    void constructor_rejectsNullRequiredFields() {
        assertThatThrownBy(() -> Ride.request(null, PICKUP, DROPOFF, VehicleType.STANDARD))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("riderId");
    }

    // ----- markAssigned -----

    @Test
    void markAssigned_fromRequested_setsAssigned() {
        Ride ride = newRequest();
        UUID driver = UUID.randomUUID();

        Ride assigned = ride.markAssigned(driver, 0.87);

        assertThat(assigned.status()).isEqualTo(RideStatus.ASSIGNED);
        assertThat(assigned.assignedDriverId()).isEqualTo(driver);
        assertThat(assigned.matchScore()).isEqualTo(0.87);
        assertThat(assigned).isNotSameAs(ride);
    }

    @Test
    void markAssigned_isNoOp_whenNotRequested() {
        Ride started = newRequest().markAssigned(UUID.randomUUID(), 0.5).markStarted();

        Ride again = started.markAssigned(UUID.randomUUID(), 0.9);

        assertThat(again).isSameAs(started);
        assertThat(again.status()).isEqualTo(RideStatus.STARTED);
    }

    // ----- markNoDrivers -----

    @Test
    void markNoDrivers_fromRequested_setsNoDriversFound() {
        Ride ride = newRequest().markNoDrivers();
        assertThat(ride.status()).isEqualTo(RideStatus.NO_DRIVERS_FOUND);
        assertThat(ride.status().isTerminal()).isTrue();
    }

    @Test
    void markNoDrivers_isNoOp_whenAlreadyAssigned() {
        Ride assigned = newRequest().markAssigned(UUID.randomUUID(), 0.5);
        assertThat(assigned.markNoDrivers()).isSameAs(assigned);
    }

    // ----- markStarted -----

    @Test
    void markStarted_fromAssigned_setsStarted() {
        Ride ride = newRequest().markAssigned(UUID.randomUUID(), 0.5).markStarted();
        assertThat(ride.status()).isEqualTo(RideStatus.STARTED);
    }

    @Test
    void markStarted_isNoOp_whenAlreadyStartedOrLater() {
        Ride started = newRequest().markAssigned(UUID.randomUUID(), 0.5).markStarted();
        assertThat(started.markStarted()).isSameAs(started);

        Ride completed = started.markCompleted(1000, 600);
        assertThat(completed.markStarted()).isSameAs(completed);
    }

    @Test
    void markStarted_isNoOp_whenTerminal() {
        Ride cancelled = newRequest().markCancelled();
        assertThat(cancelled.markStarted()).isSameAs(cancelled);
    }

    // ----- markCompleted -----

    @Test
    void markCompleted_setsFinalMetrics() {
        Ride ride = newRequest().markAssigned(UUID.randomUUID(), 0.5).markStarted()
                .markCompleted(4200, 900);

        assertThat(ride.status()).isEqualTo(RideStatus.COMPLETED);
        assertThat(ride.finalDistanceMeters()).isEqualTo(4200);
        assertThat(ride.finalDurationSeconds()).isEqualTo(900);
        assertThat(ride.status().isTerminal()).isTrue();
    }

    @Test
    void markCompleted_isNoOp_whenAlreadyCompleted() {
        Ride completed = newRequest().markCompleted(100, 50);
        Ride again = completed.markCompleted(999, 999);
        assertThat(again).isSameAs(completed);
        assertThat(again.finalDistanceMeters()).isEqualTo(100);
    }

    // ----- markCancelled -----

    @Test
    void markCancelled_fromAnyNonTerminal_setsCancelled() {
        Ride ride = newRequest().markAssigned(UUID.randomUUID(), 0.5).markCancelled();
        assertThat(ride.status()).isEqualTo(RideStatus.CANCELLED);
    }

    @Test
    void markCancelled_isNoOp_whenAlreadyTerminal() {
        Ride completed = newRequest().markCompleted(100, 50);
        assertThat(completed.markCancelled()).isSameAs(completed);
    }

    // ----- withFare -----

    @Test
    void withFare_attachesFare_independentOfStatus() {
        Ride ride = newRequest().withFare(new BigDecimal("253.50"), "INR");
        assertThat(ride.fareTotal()).isEqualByComparingTo("253.50");
        assertThat(ride.currency()).isEqualTo("INR");
        // status untouched
        assertThat(ride.status()).isEqualTo(RideStatus.REQUESTED);
    }

    @Test
    void withFare_appliesEvenWhenTerminal() {
        Ride completed = newRequest().markCompleted(100, 50);
        Ride withFare = completed.withFare(new BigDecimal("99.00"), "INR");
        assertThat(withFare.fareTotal()).isEqualByComparingTo("99.00");
        assertThat(withFare.status()).isEqualTo(RideStatus.COMPLETED);
    }
}
