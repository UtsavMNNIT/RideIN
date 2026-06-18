package com.rideflow.driver.infrastructure.persistence.jpa;

import com.rideflow.driver.application.port.out.DriverRepository;
import com.rideflow.driver.domain.exception.DriverAlreadyExistsException;
import com.rideflow.driver.domain.model.Driver;
import com.rideflow.driver.domain.model.GeoPoint;
import com.rideflow.driver.infrastructure.persistence.jpa.entity.DriverEntity;
import com.rideflow.driver.infrastructure.persistence.jpa.repository.DriverJpaRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing the {@link DriverRepository} port on top of Spring Data
 * JPA. Domain ↔ entity mapping is inlined (the shapes are nearly 1:1).
 *
 * <p>{@code save} relies on JPA merge semantics keyed by id, so it serves both
 * insert (registration) and update (presence / location). A unique-constraint
 * violation — the race two concurrent registrations can lose — is translated
 * into the domain's {@link DriverAlreadyExistsException}.
 */
@Repository
public class JpaDriverRepository implements DriverRepository {

    private final DriverJpaRepository jpa;

    public JpaDriverRepository(DriverJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return jpa.existsByPhone(phone);
    }

    @Override
    public Optional<Driver> findById(UUID id) {
        return jpa.findById(id).map(JpaDriverRepository::toDomain);
    }

    @Override
    public Optional<Driver> findByEmail(String email) {
        return jpa.findByEmail(email).map(JpaDriverRepository::toDomain);
    }

    @Override
    @Transactional
    public Driver save(Driver driver) {
        try {
            return toDomain(jpa.save(toEntity(driver)));
        } catch (DataIntegrityViolationException dup) {
            // Authoritative uniqueness guard (uq_drivers_email / uq_drivers_phone).
            throw new DriverAlreadyExistsException("email or phone", driver.email());
        }
    }

    private static DriverEntity toEntity(Driver d) {
        GeoPoint loc = d.lastLocation();
        return new DriverEntity(
                d.id(), d.email(), d.phone(), d.fullName(), d.passwordHash(),
                d.vehicleType(), d.vehiclePlate(), d.availability(),
                loc == null ? null : loc.lat(),
                loc == null ? null : loc.lng(),
                d.lastLocationAt(), d.createdAt(), d.updatedAt());
    }

    private static Driver toDomain(DriverEntity e) {
        GeoPoint loc = (e.getLastLat() == null || e.getLastLng() == null)
                ? null
                : new GeoPoint(e.getLastLat(), e.getLastLng());
        return new Driver(
                e.getId(), e.getEmail(), e.getPhone(), e.getFullName(), e.getPasswordHash(),
                e.getVehicleType(), e.getVehiclePlate(), e.getAvailability(),
                loc, e.getLastLocationAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
