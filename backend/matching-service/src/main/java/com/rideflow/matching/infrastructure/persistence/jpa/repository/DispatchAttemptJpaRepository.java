package com.rideflow.matching.infrastructure.persistence.jpa.repository;

import com.rideflow.matching.infrastructure.persistence.jpa.entity.DispatchAttemptEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DispatchAttemptJpaRepository extends JpaRepository<DispatchAttemptEntity, Long> {
}
