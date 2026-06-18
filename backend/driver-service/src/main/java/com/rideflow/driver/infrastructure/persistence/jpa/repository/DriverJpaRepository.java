package com.rideflow.driver.infrastructure.persistence.jpa.repository;

import com.rideflow.driver.infrastructure.persistence.jpa.entity.DriverEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DriverEntity}. Derived queries only —
 * the domain-facing port {@code DriverRepository} is implemented by the
 * {@code JpaDriverRepository} adapter on top of this.
 */
@Repository
public interface DriverJpaRepository extends JpaRepository<DriverEntity, UUID> {

    Optional<DriverEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
