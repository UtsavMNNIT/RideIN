package com.rideflow.matching.infrastructure.persistence.jpa;

import com.rideflow.matching.application.port.out.RideRepository;
import com.rideflow.matching.domain.model.DispatchAttempt;
import com.rideflow.matching.domain.model.GeoPoint;
import com.rideflow.matching.domain.model.Ride;
import com.rideflow.matching.infrastructure.persistence.jpa.entity.DispatchAttemptEntity;
import com.rideflow.matching.infrastructure.persistence.jpa.entity.RideEntity;
import com.rideflow.matching.infrastructure.persistence.jpa.repository.DispatchAttemptJpaRepository;
import com.rideflow.matching.infrastructure.persistence.jpa.repository.RideJpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter for {@link RideRepository}. Translates between the {@link Ride}
 * aggregate and {@code matching.rides} + {@code matching.dispatch_attempts}.
 */
@Repository
public class JpaRideRepository implements RideRepository {

    private final RideJpaRepository            rides;
    private final DispatchAttemptJpaRepository attempts;

    public JpaRideRepository(RideJpaRepository rides, DispatchAttemptJpaRepository attempts) {
        this.rides    = rides;
        this.attempts = attempts;
    }

    @Override
    public boolean existsById(UUID rideId) {
        return rides.existsById(rideId);
    }

    @Override
    public void saveOutcome(Ride ride, List<DispatchAttempt> dispatchAttempts) {
        // saveAndFlush so the partial-unique violation on assigned_driver_id
        // surfaces here (as DataIntegrityViolationException) rather than at a
        // later, harder-to-attribute commit-time flush.
        rides.saveAndFlush(toEntity(ride));

        List<DispatchAttemptEntity> rows = dispatchAttempts.stream()
                .map(a -> toEntity(ride.id(), a))
                .toList();
        attempts.saveAll(rows);
    }

    @Override
    public void updateOutcome(Ride ride, List<DispatchAttempt> dispatchAttempts) {
        // Re-dispatch path: the ride row already exists (it was ASSIGNED before
        // the offer was rejected). saveAndFlush merges by primary key so the new
        // outcome (re-assigned or failed) and bumped redispatch_count overwrite
        // the prior row, surfacing any partial-unique violation here.
        rides.saveAndFlush(toEntity(ride));

        List<DispatchAttemptEntity> rows = dispatchAttempts.stream()
                .map(a -> toEntity(ride.id(), a))
                .toList();
        attempts.saveAll(rows);
    }

    @Override
    public Optional<Ride> findById(UUID rideId) {
        return rides.findById(rideId).map(JpaRideRepository::toDomain);
    }

    // ------------------------------------------------------------------
    // mapping
    // ------------------------------------------------------------------

    private static RideEntity toEntity(Ride r) {
        return new RideEntity(
                r.id(), r.riderId(),
                r.pickup().lat(),  r.pickup().lng(),
                r.dropoff().lat(), r.dropoff().lng(),
                r.vehicleType(), r.status(),
                r.assignedDriverId(), r.assignmentScore(), r.failureReason(),
                r.requestedAt(), r.assignedAt(), r.failedAt(), r.redispatchCount());
    }

    private static DispatchAttemptEntity toEntity(UUID rideId, DispatchAttempt a) {
        return new DispatchAttemptEntity(
                rideId, a.attemptNo(), a.radiusMeters(), a.candidatesFound(),
                a.selectedDriverId(), a.selectedScore(), a.outcome(),
                a.durationMs(), a.attemptedAt());
    }

    private static Ride toDomain(RideEntity e) {
        return new Ride(
                e.getId(), e.getRiderId(),
                new GeoPoint(e.getPickupLat(),  e.getPickupLng()),
                new GeoPoint(e.getDropoffLat(), e.getDropoffLng()),
                e.getVehicleType(), e.getStatus(),
                e.getAssignedDriverId(), e.getAssignmentScore(), e.getFailureReason(),
                e.getRequestedAt(), e.getAssignedAt(), e.getFailedAt(), e.getRedispatchCount());
    }
}
