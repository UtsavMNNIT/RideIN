package com.rideflow.trip.application.usecase;

import com.rideflow.trip.domain.exception.IllegalTripTransitionException;
import com.rideflow.trip.domain.exception.TripActionForbiddenException;
import com.rideflow.trip.domain.exception.TripNotFoundException;
import com.rideflow.trip.domain.model.CancelledBy;
import com.rideflow.trip.domain.model.GeoPoint;
import com.rideflow.trip.domain.model.Trip;
import com.rideflow.trip.domain.model.TripMetrics;
import com.rideflow.trip.domain.model.TripStatus;
import com.rideflow.trip.domain.model.VehicleType;
import com.rideflow.trip.support.InMemoryTripRepository;
import com.rideflow.trip.support.RecordingTripEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripActionServiceTest {

    private static final UUID RIDE   = UUID.randomUUID();
    private static final UUID RIDER  = UUID.randomUUID();
    private static final UUID DRIVER = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();
    private static final Instant T0 = Instant.parse("2026-06-20T10:00:00Z");

    private InMemoryTripRepository repo;
    private RecordingTripEventPublisher events;
    private TripActionService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryTripRepository();
        events = new RecordingTripEventPublisher();
        service = new TripActionService(repo, events);
    }

    private Trip offered() {
        return Trip.offer(RIDE, RIDER, DRIVER,
                new GeoPoint(12.9, 77.5), new GeoPoint(13.0, 77.6),
                VehicleType.STANDARD, 0.9, T0, T0.plusSeconds(30));
    }

    private void seedOffered() {
        repo.seed(offered());
    }

    // ------------------------------------------------------------------
    // get
    // ------------------------------------------------------------------

    @Test
    void get_byRider_returnsTrip() {
        seedOffered();
        assertThat(service.get(RIDE, RIDER).id()).isEqualTo(RIDE);
    }

    @Test
    void get_byDriver_returnsTrip() {
        seedOffered();
        assertThat(service.get(RIDE, DRIVER).id()).isEqualTo(RIDE);
    }

    @Test
    void get_byStranger_forbidden() {
        seedOffered();
        assertThatThrownBy(() -> service.get(RIDE, STRANGER))
                .isInstanceOf(TripActionForbiddenException.class);
    }

    @Test
    void get_notFound_throws() {
        assertThatThrownBy(() -> service.get(RIDE, RIDER))
                .isInstanceOf(TripNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // accept
    // ------------------------------------------------------------------

    @Test
    void accept_publishesRideAccepted_andPersists() {
        seedOffered();
        Trip next = service.accept(RIDE, DRIVER);

        assertThat(next.status()).isEqualTo(TripStatus.ACCEPTED);
        assertThat(repo.findById(RIDE).orElseThrow().status()).isEqualTo(TripStatus.ACCEPTED);
        assertThat(events.accepted).hasSize(1);
        assertThat(events.total()).isEqualTo(1);
        var ev = events.accepted.get(0);
        assertThat(ev.rideId()).isEqualTo(RIDE);
        assertThat(ev.riderId()).isEqualTo(RIDER);
        assertThat(ev.driverId()).isEqualTo(DRIVER);
        assertThat(ev.acceptedAt()).isEqualTo(next.acceptedAt());
    }

    @Test
    void accept_wrongDriver_forbidden_noEvent() {
        seedOffered();
        assertThatThrownBy(() -> service.accept(RIDE, STRANGER))
                .isInstanceOf(TripActionForbiddenException.class);
        assertThat(events.total()).isZero();
        assertThat(repo.findById(RIDE).orElseThrow().status()).isEqualTo(TripStatus.OFFERED);
    }

    @Test
    void accept_notFound_throws() {
        assertThatThrownBy(() -> service.accept(RIDE, DRIVER))
                .isInstanceOf(TripNotFoundException.class);
    }

    @Test
    void accept_alreadyAccepted_illegalTransition() {
        seedOffered();
        service.accept(RIDE, DRIVER);
        assertThatThrownBy(() -> service.accept(RIDE, DRIVER))
                .isInstanceOf(IllegalTripTransitionException.class);
    }

    // ------------------------------------------------------------------
    // reject / expire (both publish RideRejected)
    // ------------------------------------------------------------------

    @Test
    void reject_publishesRideRejected_withDriverDeclined() {
        seedOffered();
        Trip next = service.reject(RIDE, DRIVER);

        assertThat(next.status()).isEqualTo(TripStatus.REJECTED);
        assertThat(events.rejected).hasSize(1);
        assertThat(events.rejected.get(0).reason()).isEqualTo(Trip.REASON_DRIVER_DECLINED);
    }

    @Test
    void reject_wrongDriver_forbidden() {
        seedOffered();
        assertThatThrownBy(() -> service.reject(RIDE, STRANGER))
                .isInstanceOf(TripActionForbiddenException.class);
        assertThat(events.total()).isZero();
    }

    @Test
    void expire_publishesRideRejected_withExpiredReason() {
        seedOffered();
        service.expire(RIDE);

        assertThat(repo.findById(RIDE).orElseThrow().status()).isEqualTo(TripStatus.EXPIRED);
        assertThat(events.rejected).hasSize(1);
        assertThat(events.rejected.get(0).reason()).isEqualTo(Trip.REASON_EXPIRED);
    }

    @Test
    void expire_notFound_throws() {
        assertThatThrownBy(() -> service.expire(RIDE))
                .isInstanceOf(TripNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // arrive (internal, no event)
    // ------------------------------------------------------------------

    @Test
    void arrive_noEventPublished() {
        seedOffered();
        service.accept(RIDE, DRIVER);
        events.accepted.clear();

        Trip next = service.arrive(RIDE, DRIVER);
        assertThat(next.status()).isEqualTo(TripStatus.ARRIVED);
        assertThat(events.total()).isZero();
    }

    @Test
    void arrive_wrongDriver_forbidden() {
        seedOffered();
        service.accept(RIDE, DRIVER);
        assertThatThrownBy(() -> service.arrive(RIDE, STRANGER))
                .isInstanceOf(TripActionForbiddenException.class);
    }

    // ------------------------------------------------------------------
    // start
    // ------------------------------------------------------------------

    @Test
    void start_publishesRideStarted() {
        seedOffered();
        service.accept(RIDE, DRIVER);
        service.arrive(RIDE, DRIVER);

        Trip next = service.start(RIDE, DRIVER);
        assertThat(next.status()).isEqualTo(TripStatus.STARTED);
        assertThat(events.started).hasSize(1);
        var ev = events.started.get(0);
        assertThat(ev.rideId()).isEqualTo(RIDE);
        assertThat(ev.distanceMeters()).isNull();
        assertThat(ev.durationSeconds()).isNull();
    }

    // ------------------------------------------------------------------
    // complete
    // ------------------------------------------------------------------

    @Test
    void complete_publishesRideCompleted_withMetrics() {
        seedOffered();
        service.accept(RIDE, DRIVER);
        service.arrive(RIDE, DRIVER);
        service.start(RIDE, DRIVER);

        Trip next = service.complete(RIDE, DRIVER, new TripMetrics(4321, 765));
        assertThat(next.status()).isEqualTo(TripStatus.COMPLETED);
        assertThat(events.completed).hasSize(1);
        var ev = events.completed.get(0);
        assertThat(ev.distanceMeters()).isEqualTo(4321);
        assertThat(ev.durationSeconds()).isEqualTo(765);
        assertThat(ev.fareTotal()).isNull();
        assertThat(ev.currency()).isNull();
    }

    @Test
    void complete_wrongDriver_forbidden() {
        seedOffered();
        service.accept(RIDE, DRIVER);
        service.arrive(RIDE, DRIVER);
        service.start(RIDE, DRIVER);
        assertThatThrownBy(() -> service.complete(RIDE, STRANGER, new TripMetrics(1, 1)))
                .isInstanceOf(TripActionForbiddenException.class);
    }

    // ------------------------------------------------------------------
    // cancel — CancelledBy resolution edges
    // ------------------------------------------------------------------

    @Test
    void cancel_byRiderRole_resolvesRider() {
        seedOffered();
        Trip next = service.cancel(RIDE, RIDER, "RIDER", "changed mind");
        assertThat(next.status()).isEqualTo(TripStatus.CANCELLED);
        assertThat(events.cancelled).hasSize(1);
        var ev = events.cancelled.get(0);
        assertThat(ev.cancelledBy()).isEqualTo(CancelledBy.RIDER.name());
        assertThat(ev.reason()).isEqualTo("changed mind");
    }

    @Test
    void cancel_byDriverRole_resolvesDriver() {
        seedOffered();
        service.accept(RIDE, DRIVER);
        Trip next = service.cancel(RIDE, DRIVER, "driver", "vehicle issue"); // case-insensitive role
        assertThat(next.cancelledBy()).isEqualTo(CancelledBy.DRIVER);
        assertThat(events.cancelled.get(0).cancelledBy()).isEqualTo("DRIVER");
    }

    @Test
    void cancel_riderRoleButDriverId_forbidden() {
        seedOffered();
        // Caller claims RIDER role but the id is the driver's — not the rider.
        assertThatThrownBy(() -> service.cancel(RIDE, DRIVER, "RIDER", "x"))
                .isInstanceOf(TripActionForbiddenException.class);
        assertThat(events.total()).isZero();
    }

    @Test
    void cancel_driverRoleButRiderId_forbidden() {
        seedOffered();
        assertThatThrownBy(() -> service.cancel(RIDE, RIDER, "DRIVER", "x"))
                .isInstanceOf(TripActionForbiddenException.class);
    }

    @Test
    void cancel_unknownRole_forbidden() {
        seedOffered();
        assertThatThrownBy(() -> service.cancel(RIDE, RIDER, "ADMIN", "x"))
                .isInstanceOf(TripActionForbiddenException.class);
    }

    @Test
    void cancel_strangerId_forbidden() {
        seedOffered();
        assertThatThrownBy(() -> service.cancel(RIDE, STRANGER, "RIDER", "x"))
                .isInstanceOf(TripActionForbiddenException.class);
    }

    @Test
    void cancel_notFound_throws() {
        assertThatThrownBy(() -> service.cancel(RIDE, RIDER, "RIDER", "x"))
                .isInstanceOf(TripNotFoundException.class);
    }

    @Test
    void cancel_terminalTrip_illegalTransition() {
        seedOffered();
        service.reject(RIDE, DRIVER); // now REJECTED (terminal)
        events.rejected.clear();
        assertThatThrownBy(() -> service.cancel(RIDE, DRIVER, "DRIVER", "x"))
                .isInstanceOf(IllegalTripTransitionException.class);
    }
}
