package com.rideflow.rider.infrastructure.persistence.jpa.repository;

import com.rideflow.rider.infrastructure.persistence.jpa.entity.RiderEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiderJpaRepository extends JpaRepository<RiderEntity, UUID> {

    Optional<RiderEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
