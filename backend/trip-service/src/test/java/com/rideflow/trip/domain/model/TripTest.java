package com.rideflow.trip.domain.model;

import com.rideflow.trip.domain.exception.IllegalTripTransitionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure unit tests for the {@link Trip} state machine. */
class TripTest {

    private static final UUID RIDE   = UUID.randomUUID();
    private static final UUID RIDER  = UUID.randomUUID();
    private static final UUID DRIVER = UUID.randomUUID();
    private static final GeoPoint PICKUP  = new GeoPoint(12.9, 77.5);
    private static final GeoPoint DROPOFF = new GeoPoint(13.0, 77.6);
    private static final Instant T0 = Instant.parse("2026-06-20T10:00:00Z");

    private Trip offered() {
        return Trip.offer(RIDE, RIDER, DRIVER, PICKUP, DROPOFF, VehicleType.STANDARD,
                0.87, T0, T0.plusSeconds(30));
    }

    @Test
    void offer_createsOfferedTrip() {
        Trip t = offered();
        assertThat(t.id()).isEqualTo(RIDE);
        assertThat(t.status()).isEqualTo(TripStatus.OFFERED);
        assertThat(t.matchScore()).isEqualTo(0.87);
        assertThat(t.offeredAt()).isEqualTo(T0);
        assertThat(t.offerExpiresAt()).isEqualTo(T0.plusSeconds(30));
        assertThat(t.acceptedAt()).isNull();
    }

    // ------------------------------------------------------------------
    // Legal transitions
    // ------------------------------------------------------------------

    @Nested
    class LegalTransitions {

        @Test
        void accept() {
            Instant when = T0.plusSeconds(5);
            Trip next = offered().accept(when);
            assertThat(next.status()).isEqualTo(TripStatus.ACCEPTED);
            assertThat(next.acceptedAt()).isEqualTo(when);
        }

        @Test
        void reject_setsDriverDeclinedReason() {
            Instant when = T0.plusSeconds(5);
            Trip next = offered().reject(when);
            assertThat(next.status()).isEqualTo(TripStatus.REJECTED);
            assertThat(next.rejectReason()).isEqualTo(Trip.REASON_DRIVER_DECLINED);
            assertThat(next.rejectedAt()).isEqualTo(when);
        }

        @Test
        void expire_setsExpiredReason() {
            Instant when = T0.plusSeconds(31);
            Trip next = offered().expire(when);
            assertThat(next.status()).isEqualTo(TripStatus.EXPIRED);
            assertThat(next.rejectReason()).isEqualTo(Trip.REASON_EXPIRED);
            assertThat(next.rejectedAt()).isEqualTo(when);
        }

        @Test
        void arrive() {
            Trip next = offered().accept(T0).arrive(T0.plusSeconds(60));
            assertThat(next.status()).isEqualTo(TripStatus.ARRIVED);
            assertThat(next.arrivedAt()).isEqualTo(T0.plusSeconds(60));
        }

        @Test
        void start() {
            Trip next = offered().accept(T0).arrive(T0.plusSeconds(60)).start(T0.plusSeconds(70));
            assertThat(next.status()).isEqualTo(TripStatus.STARTED);
            assertThat(next.startedAt()).isEqualTo(T0.plusSeconds(70));
        }

        @Test
        void complete_carriesMetrics() {
            Trip started = offered().accept(T0).arrive(T0.plusSeconds(60)).start(T0.plusSeconds(70));
            Trip next = started.complete(new TripMetrics(4200, 900), T0.plusSeconds(900));
            assertThat(next.status()).isEqualTo(TripStatus.COMPLETED);
            assertThat(next.completedAt()).isEqualTo(T0.plusSeconds(900));
            assertThat(next.finalDistanceMeters()).isEqualTo(4200);
            assertThat(next.finalDurationSeconds()).isEqualTo(900);
        }

        @Test
        void fullHappyPath() {
            Trip t = offered()
                    .accept(T0.plusSeconds(2))
                    .arrive(T0.plusSeconds(120))
                    .start(T0.plusSeconds(130))
                    .complete(new TripMetrics(5000, 1000), T0.plusSeconds(1130));
            assertThat(t.status()).isEqualTo(TripStatus.COMPLETED);
        }
    }

    // ------------------------------------------------------------------
    // Cancellation (legal from any non-terminal state)
    // ------------------------------------------------------------------

    @Nested
    class Cancellation {

        @Test
        void cancelFromOffered() {
            Trip next = offered().cancel(CancelledBy.RIDER, "changed mind", T0.plusSeconds(3));
            assertThat(next.status()).isEqualTo(TripStatus.CANCELLED);
            assertThat(next.cancelledBy()).isEqualTo(CancelledBy.RIDER);
            assertThat(next.cancelReason()).isEqualTo("changed mind");
            assertThat(next.cancelledAt()).isEqualTo(T0.plusSeconds(3));
        }

        @Test
        void cancelFromAccepted() {
            Trip next = offered().accept(T0).cancel(CancelledBy.DRIVER, "vehicle issue", T0.plusSeconds(10));
            assertThat(next.status()).isEqualTo(TripStatus.CANCELLED);
            assertThat(next.cancelledBy()).isEqualTo(CancelledBy.DRIVER);
        }

        @Test
        void cancelFromArrived() {
            Trip next = offered().accept(T0).arrive(T0.plusSeconds(60))
                    .cancel(CancelledBy.RIDER, null, T0.plusSeconds(65));
            assertThat(next.status()).isEqualTo(TripStatus.CANCELLED);
            assertThat(next.cancelReason()).isNull();
        }

        @Test
        void cancelFromStarted() {
            Trip next = offered().accept(T0).arrive(T0.plusSeconds(60)).start(T0.plusSeconds(70))
                    .cancel(CancelledBy.SYSTEM, "ops", T0.plusSeconds(80));
            assertThat(next.status()).isEqualTo(TripStatus.CANCELLED);
            assertThat(next.cancelledBy()).isEqualTo(CancelledBy.SYSTEM);
        }

        @Test
        void cancelFromTerminalThrows() {
            Trip completed = offered().accept(T0).arrive(T0.plusSeconds(60)).start(T0.plusSeconds(70))
                    .complete(new TripMetrics(1, 1), T0.plusSeconds(200));
            assertThatThrownBy(() -> completed.cancel(CancelledBy.RIDER, "x", T0.plusSeconds(300)))
                    .isInstanceOf(IllegalTripTransitionException.class)
                    .hasMessageContaining("cancel")
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        void cancelNullCancelledByThrows() {
            Trip t = offered();
            assertThatThrownBy(() -> t.cancel(null, "x", T0))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ------------------------------------------------------------------
    // Illegal transitions
    // ------------------------------------------------------------------

    @Nested
    class IllegalTransitions {

        @Test
        void acceptFromAcceptedThrows() {
            Trip accepted = offered().accept(T0);
            assertThatThrownBy(() -> accepted.accept(T0.plusSeconds(1)))
                    .isInstanceOf(IllegalTripTransitionException.class)
                    .hasMessageContaining("accept")
                    .hasMessageContaining("ACCEPTED");
        }

        @Test
        void rejectFromAcceptedThrows() {
            Trip accepted = offered().accept(T0);
            assertThatThrownBy(() -> accepted.reject(T0))
                    .isInstanceOf(IllegalTripTransitionException.class);
        }

        @Test
        void expireFromAcceptedThrows() {
            Trip accepted = offered().accept(T0);
            assertThatThrownBy(() -> accepted.expire(T0))
                    .isInstanceOf(IllegalTripTransitionException.class);
        }

        @Test
        void arriveFromOfferedThrows() {
            Trip t = offered();
            assertThatThrownBy(() -> t.arrive(T0))
                    .isInstanceOf(IllegalTripTransitionException.class)
                    .hasMessageContaining("mark-arrived");
        }

        @Test
        void startFromAcceptedThrows() {
            Trip accepted = offered().accept(T0);
            assertThatThrownBy(() -> accepted.start(T0))
                    .isInstanceOf(IllegalTripTransitionException.class)
                    .hasMessageContaining("start");
        }

        @Test
        void completeFromArrivedThrows() {
            Trip arrived = offered().accept(T0).arrive(T0.plusSeconds(60));
            assertThatThrownBy(() -> arrived.complete(new TripMetrics(1, 1), T0))
                    .isInstanceOf(IllegalTripTransitionException.class)
                    .hasMessageContaining("complete");
        }

        @Test
        void completeNullMetricsThrows() {
            Trip started = offered().accept(T0).arrive(T0.plusSeconds(60)).start(T0.plusSeconds(70));
            assertThatThrownBy(() -> started.complete(null, T0))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ------------------------------------------------------------------
    // Immutability
    // ------------------------------------------------------------------

    @Test
    void transitionsReturnNewImmutableInstances() {
        Trip original = offered();
        Trip accepted = original.accept(T0);
        assertThat(accepted).isNotSameAs(original);
        assertThat(original.status()).isEqualTo(TripStatus.OFFERED);
        assertThat(accepted.status()).isEqualTo(TripStatus.ACCEPTED);
        // Identity & immutable fields preserved across transition.
        assertThat(accepted.id()).isEqualTo(original.id());
        assertThat(accepted.riderId()).isEqualTo(original.riderId());
        assertThat(accepted.driverId()).isEqualTo(original.driverId());
    }
}
