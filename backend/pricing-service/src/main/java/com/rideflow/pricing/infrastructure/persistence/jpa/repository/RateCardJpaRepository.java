package com.rideflow.pricing.infrastructure.persistence.jpa.repository;

import com.rideflow.pricing.domain.model.VehicleType;
import com.rideflow.pricing.infrastructure.persistence.jpa.entity.RateCardEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RateCardJpaRepository extends JpaRepository<RateCardEntity, VehicleType> {
}
