package com.rideflow.pricing.infrastructure.persistence.jpa.repository;

import com.rideflow.pricing.infrastructure.persistence.jpa.entity.QuoteEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuoteJpaRepository extends JpaRepository<QuoteEntity, UUID> {

    Optional<QuoteEntity> findByRideId(UUID rideId);

    boolean existsByRideId(UUID rideId);
}
