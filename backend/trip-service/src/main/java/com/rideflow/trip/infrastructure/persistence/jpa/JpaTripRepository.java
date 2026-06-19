package com.rideflow.trip.infrastructure.persistence.jpa;

import com.rideflow.trip.application.port.out.TripRepository;
import com.rideflow.trip.domain.model.GeoPoint;
import com.rideflow.trip.domain.model.Trip;
import com.rideflow.trip.domain.model.VehicleType;
import com.rideflow.trip.infrastructure.persistence.jpa.entity.TripEntity;
import com.rideflow.trip.infrastructure.persistence.jpa.repository.TripJpaRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter for {@link TripRepository}. Maps {@link Trip} ↔ {@link TripEntity}.
 *
 * <p><b>Insert vs update.</b> An OFFER is a fresh row (insert). Every later
 * transition is an update of the <em>managed</em> entity already in the
 * persistence context (loaded earlier in the same transaction), so Hibernate
 * dirty-checks it and bumps {@code @Version}. Building a new version-0 entity for
 * an update would spuriously trip the optimistic-lock check.
 */
@Repository
public class JpaTripRepository implements TripRepository {

    private final TripJpaRepository jpa;

    public JpaTripRepository(TripJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Trip save(Trip trip) {
        TripEntity existing = jpa.findById(trip.id()).orElse(null);
        if (existing == null) {
            return toDomain(jpa.saveAndFlush(toNewEntity(trip)));
        }
        existing.applyState(trip);
        return toDomain(jpa.saveAndFlush(existing));
    }

    @Override
    public Optional<Trip> findById(UUID rideId) {
        return jpa.findById(rideId).map(JpaTripRepository::toDomain);
    }

    @Override
    public boolean existsById(UUID rideId) {
        return jpa.existsById(rideId);
    }

    @Override
    public List<UUID> findExpiredOfferIds(Instant cutoff, int limit) {
        return jpa.findExpiredOfferIds(cutoff, PageRequest.of(0, limit));
    }

    private static TripEntity toNewEntity(Trip t) {
        return new TripEntity(
                t.id(), t.riderId(), t.driverId(),
                t.pickup().lat(), t.pickup().lng(), t.dropoff().lat(), t.dropoff().lng(),
                t.vehicleType().name(), t.status(), t.matchScore(),
                t.rejectReason(), t.cancelledBy(), t.cancelReason(),
                t.finalDistanceMeters(), t.finalDurationSeconds(),
                t.offeredAt(), t.offerExpiresAt(), t.acceptedAt(), t.arrivedAt(),
                t.startedAt(), t.completedAt(), t.rejectedAt(), t.cancelledAt());
    }

    private static Trip toDomain(TripEntity e) {
        return new Trip(
                e.getId(), e.getRiderId(), e.getDriverId(),
                new GeoPoint(e.getPickupLat(), e.getPickupLng()),
                new GeoPoint(e.getDropoffLat(), e.getDropoffLng()),
                VehicleType.valueOf(e.getVehicleType()),
                e.getStatus(), e.getMatchScore(),
                e.getRejectReason(),
                e.getCancelledBy(),
                e.getCancelReason(),
                e.getFinalDistanceMeters(), e.getFinalDurationSeconds(),
                e.getOfferedAt(), e.getOfferExpiresAt(), e.getAcceptedAt(), e.getArrivedAt(),
                e.getStartedAt(), e.getCompletedAt(), e.getRejectedAt(), e.getCancelledAt());
    }
}
