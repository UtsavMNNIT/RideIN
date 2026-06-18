package com.rideflow.rider.infrastructure.persistence.jpa;

import com.rideflow.rider.application.port.out.RiderRepository;
import com.rideflow.rider.domain.exception.RiderAlreadyExistsException;
import com.rideflow.rider.domain.model.Rider;
import com.rideflow.rider.infrastructure.persistence.jpa.entity.RiderEntity;
import com.rideflow.rider.infrastructure.persistence.jpa.repository.RiderJpaRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing {@link RiderRepository} on top of Spring Data JPA.
 * Domain ↔ entity mapping is inlined (shapes are 1:1).
 */
@Repository
public class JpaRiderRepository implements RiderRepository {

    private final RiderJpaRepository jpa;

    public JpaRiderRepository(RiderJpaRepository jpa) {
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
    public boolean existsById(UUID id) {
        return jpa.existsById(id);
    }

    @Override
    public Optional<Rider> findById(UUID id) {
        return jpa.findById(id).map(JpaRiderRepository::toDomain);
    }

    @Override
    public Optional<Rider> findByEmail(String email) {
        return jpa.findByEmail(email).map(JpaRiderRepository::toDomain);
    }

    @Override
    @Transactional
    public Rider save(Rider rider) {
        try {
            return toDomain(jpa.save(toEntity(rider)));
        } catch (DataIntegrityViolationException dup) {
            throw new RiderAlreadyExistsException("email or phone", rider.email());
        }
    }

    private static RiderEntity toEntity(Rider r) {
        return new RiderEntity(
                r.id(), r.email(), r.phone(), r.fullName(),
                r.passwordHash(), r.createdAt(), r.updatedAt());
    }

    private static Rider toDomain(RiderEntity e) {
        return new Rider(
                e.getId(), e.getEmail(), e.getPhone(), e.getFullName(),
                e.getPasswordHash(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
