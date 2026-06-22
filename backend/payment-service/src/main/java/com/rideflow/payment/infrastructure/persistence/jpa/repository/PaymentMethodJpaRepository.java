package com.rideflow.payment.infrastructure.persistence.jpa.repository;

import com.rideflow.payment.infrastructure.persistence.jpa.entity.PaymentMethodEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentMethodJpaRepository extends JpaRepository<PaymentMethodEntity, UUID> {

    List<PaymentMethodEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
