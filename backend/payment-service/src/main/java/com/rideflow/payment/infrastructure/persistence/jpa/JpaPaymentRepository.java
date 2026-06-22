package com.rideflow.payment.infrastructure.persistence.jpa;

import com.rideflow.payment.application.port.out.PaymentRepository;
import com.rideflow.payment.domain.model.Money;
import com.rideflow.payment.domain.model.Payment;
import com.rideflow.payment.infrastructure.persistence.jpa.entity.PaymentEntity;
import com.rideflow.payment.infrastructure.persistence.jpa.repository.PaymentJpaRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter for {@link PaymentRepository}. Maps {@link Payment} ↔
 * {@link PaymentEntity}.
 *
 * <p><b>Insert vs update.</b> Opening a payment is a fresh row (insert). A
 * settle/cancel is an update of the <em>managed</em> entity already in the
 * persistence context, so Hibernate dirty-checks it and bumps {@code @Version}.
 */
@Repository
public class JpaPaymentRepository implements PaymentRepository {

    private final PaymentJpaRepository jpa;

    public JpaPaymentRepository(PaymentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentEntity existing = jpa.findById(payment.id()).orElse(null);
        if (existing == null) {
            return toDomain(jpa.saveAndFlush(toNewEntity(payment)));
        }
        existing.applyState(payment);
        return toDomain(jpa.saveAndFlush(existing));
    }

    @Override
    public Optional<Payment> findByRideId(UUID rideId) {
        return jpa.findByRideId(rideId).map(JpaPaymentRepository::toDomain);
    }

    @Override
    public boolean existsByRideId(UUID rideId) {
        return jpa.existsByRideId(rideId);
    }

    private static PaymentEntity toNewEntity(Payment p) {
        return new PaymentEntity(
                p.id(), p.rideId(), p.riderId(), p.driverId(),
                p.fare().amount(), p.fare().currency(), p.status(),
                p.paymentMethodId(), p.failureReason(), p.settledAt(), p.createdAt());
    }

    private static Payment toDomain(PaymentEntity e) {
        return new Payment(
                e.getId(), e.getRideId(), e.getRiderId(), e.getDriverId(),
                Money.of(e.getAmount(), e.getCurrency()), e.getStatus(),
                e.getPaymentMethodId(), e.getFailureReason(),
                e.getCreatedAt(), e.getSettledAt());
    }
}
