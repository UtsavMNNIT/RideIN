package com.rideflow.rider.infrastructure.persistence.jpa;

import com.rideflow.rider.application.port.out.RideRepository;
import com.rideflow.rider.domain.model.GeoPoint;
import com.rideflow.rider.domain.model.Ride;
import com.rideflow.rider.infrastructure.persistence.jpa.entity.RideEntity;
import com.rideflow.rider.infrastructure.persistence.jpa.repository.RideJpaRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing {@link RideRepository} on top of Spring Data JPA.
 */
@Repository
public class JpaRideRepository implements RideRepository {

    private final RideJpaRepository jpa;

    public JpaRideRepository(RideJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Ride save(Ride ride) {
        return toDomain(jpa.save(toEntity(ride)));
    }

    @Override
    public Optional<Ride> findById(UUID rideId) {
        return jpa.findById(rideId).map(JpaRideRepository::toDomain);
    }

    @Override
    public List<Ride> findByRider(UUID riderId, int page, int size) {
        return jpa.findByRider(riderId, PageRequest.of(page, size))
                  .stream()
                  .map(JpaRideRepository::toDomain)
                  .toList();
    }

    private static RideEntity toEntity(Ride r) {
        return new RideEntity(
                r.id(), r.riderId(),
                r.pickup().lat(),  r.pickup().lng(),
                r.dropoff().lat(), r.dropoff().lng(),
                r.vehicleType(), r.status(),
                r.assignedDriverId(), r.matchScore(), r.fareTotal(), r.currency(),
                r.finalDistanceMeters(), r.finalDurationSeconds(),
                r.requestedAt(), r.createdAt(), r.updatedAt());
    }

    private static Ride toDomain(RideEntity e) {
        return new Ride(
                e.getId(), e.getRiderId(),
                new GeoPoint(e.getPickupLat(),  e.getPickupLng()),
                new GeoPoint(e.getDropoffLat(), e.getDropoffLng()),
                e.getVehicleType(), e.getStatus(),
                e.getAssignedDriverId(), e.getMatchScore(), e.getFareTotal(), e.getCurrency(),
                e.getFinalDistanceMeters(), e.getFinalDurationSeconds(),
                e.getRequestedAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
