package com.rideflow.matching.infrastructure.persistence.jpa.repository;

import com.rideflow.matching.infrastructure.persistence.jpa.entity.RideEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RideJpaRepository extends JpaRepository<RideEntity, UUID> {
}
