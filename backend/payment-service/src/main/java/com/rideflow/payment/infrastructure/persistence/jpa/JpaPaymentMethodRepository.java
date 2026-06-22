package com.rideflow.payment.infrastructure.persistence.jpa;

import com.rideflow.payment.application.port.out.PaymentMethodRepository;
import com.rideflow.payment.domain.model.PaymentMethod;
import com.rideflow.payment.infrastructure.persistence.jpa.entity.PaymentMethodEntity;
import com.rideflow.payment.infrastructure.persistence.jpa.repository.PaymentMethodJpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** JPA adapter for {@link PaymentMethodRepository}. */
@Repository
public class JpaPaymentMethodRepository implements PaymentMethodRepository {

    private final PaymentMethodJpaRepository jpa;

    public JpaPaymentMethodRepository(PaymentMethodJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public PaymentMethod save(PaymentMethod m) {
        jpa.save(new PaymentMethodEntity(
                m.id(), m.userId(), m.brand(), m.last4(), m.token(), m.isDefault()));
        return m;   // domain already carries id/token/createdAt
    }

    @Override
    public List<PaymentMethod> findByUser(UUID userId) {
        return jpa.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(JpaPaymentMethodRepository::toDomain)
                .toList();
    }

    private static PaymentMethod toDomain(PaymentMethodEntity e) {
        return new PaymentMethod(
                e.getId(), e.getUserId(), e.getBrand(), e.getLast4(),
                e.getToken(), e.isDefault(), e.getCreatedAt());
    }
}
