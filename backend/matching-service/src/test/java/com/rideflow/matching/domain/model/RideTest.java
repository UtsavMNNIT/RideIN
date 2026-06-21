package com.rideflow.matching.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for the {@link Ride} dispatch state machine. */
class RideTest {

    private static final GeoPoint PICKUP = new GeoPoint(12.97, 77.59);
    private static final GeoPoint DROPOFF = new GeoPoint(13.00, 77.62);
    private static final Instant T0 = Instant.parse("2026-06-20T10:00:00Z");

    private static Ride requested() {
        return Ride.requested(UUID.randomUUID(), UUID.randomUUID(),
                PICKUP, DROPOFF, VehicleType.STANDARD, T0);
    }

    @Test
    void requested_isInitialState() {
        Ride r = requested();
        assertThat(r.status()).isEqualTo(RideStatus.REQUESTED);
        assertThat(r.assignedDriverId()).isNull();
        assertThat(r.redispatchCount()).isZero();
    }

    @Test
    void beginDispatch_fromRequested_movesToDispatching() {
        Ride r = requested().beginDispatch();
        assertThat(r.status()).isEqualTo(RideStatus.DISPATCHING);
    }

    @Test
    void beginDispatch_fromNonRequested_isIllegal() {
        Ride dispatching = requested().beginDispatch();
        assertThatThrownBy(dispatching::beginDispatch)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("illegal transition");
    }

    @Test
    void assignTo_setsDriverScoreAndTimestamp() {
        UUID driver = UUID.randomUUID();
        Instant when = T0.plusSeconds(2);
        Ride r = requested().beginDispatch().assignTo(driver, 0.87, when);

        assertThat(r.status()).isEqualTo(RideStatus.ASSIGNED);
        assertThat(r.assignedDriverId()).isEqualTo(driver);
        assertThat(r.assignmentScore()).isEqualTo(0.87);
        assertThat(r.assignedAt()).isEqualTo(when);
        assertThat(r.redispatchCount()).isZero();
    }

    @Test
    void assignTo_nullDriver_isRejected() {
        Ride dispatching = requested().beginDispatch();
        assertThatThrownBy(() -> dispatching.assignTo(null, 0.5, T0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fail_setsReasonAndTimestamp() {
        Instant when = T0.plusSeconds(3);
        Ride r = requested().beginDispatch().fail("NO_DRIVERS_IN_RANGE", when);

        assertThat(r.status()).isEqualTo(RideStatus.DISPATCH_FAILED);
        assertThat(r.failureReason()).isEqualTo("NO_DRIVERS_IN_RANGE");
        assertThat(r.failedAt()).isEqualTo(when);
        assertThat(r.assignedDriverId()).isNull();
    }

    @Test
    void beginRedispatch_fromAssigned_resetsToDispatchingPreservingCount() {
        Ride assigned = requested().beginDispatch()
                .assignTo(UUID.randomUUID(), 0.9, T0.plusSeconds(1));

        Ride redispatching = assigned.beginRedispatch();
        assertThat(redispatching.status()).isEqualTo(RideStatus.DISPATCHING);
        assertThat(redispatching.assignedDriverId()).isNull();
        assertThat(redispatching.assignmentScore()).isNull();
        assertThat(redispatching.redispatchCount()).isEqualTo(assigned.redispatchCount());
        assertThat(redispatching.id()).isEqualTo(assigned.id());
    }

    @Test
    void reassignTo_incrementsRedispatchCount() {
        UUID firstDriver = UUID.randomUUID();
        UUID secondDriver = UUID.randomUUID();

        Ride assigned = requested().beginDispatch().assignTo(firstDriver, 0.8, T0.plusSeconds(1));
        Ride redispatching = assigned.beginRedispatch();
        Ride reassigned = redispatching.reassignTo(secondDriver, 0.75, T0.plusSeconds(5));

        assertThat(reassigned.status()).isEqualTo(RideStatus.ASSIGNED);
        assertThat(reassigned.assignedDriverId()).isEqualTo(secondDriver);
        assertThat(reassigned.redispatchCount()).isEqualTo(1);

        // A second re-dispatch cycle increments again.
        Ride twice = reassigned.beginRedispatch().reassignTo(UUID.randomUUID(), 0.7, T0.plusSeconds(9));
        assertThat(twice.redispatchCount()).isEqualTo(2);
    }

    @Test
    void reassignTo_nullDriver_isRejected() {
        Ride redispatching = requested().beginDispatch()
                .assignTo(UUID.randomUUID(), 0.8, T0).beginRedispatch();
        assertThatThrownBy(() -> redispatching.reassignTo(null, 0.5, T0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsMissingRequiredFields() {
        assertThatThrownBy(() -> Ride.requested(null, UUID.randomUUID(), PICKUP, DROPOFF, VehicleType.STANDARD, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Ride.requested(UUID.randomUUID(), null, PICKUP, DROPOFF, VehicleType.STANDARD, T0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeRedispatchCount_isRejected() {
        assertThatThrownBy(() -> new Ride(UUID.randomUUID(), UUID.randomUUID(), PICKUP, DROPOFF,
                VehicleType.STANDARD, RideStatus.REQUESTED, null, null, null, T0, null, null, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
