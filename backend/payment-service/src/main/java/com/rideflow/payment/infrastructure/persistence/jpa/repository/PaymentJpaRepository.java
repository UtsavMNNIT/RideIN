package com.rideflow.payment.infrastructure.persistence.jpa.repository;

import com.rideflow.payment.infrastructure.persistence.jpa.entity.PaymentEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByRideId(UUID rideId);

    boolean existsByRideId(UUID rideId);
}
