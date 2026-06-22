package com.rideflow.payment.support;

import com.rideflow.payment.application.port.out.PaymentRepository;
import com.rideflow.payment.domain.model.Payment;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory {@link PaymentRepository} for use-case tests. Keyed by rideId. */
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<UUID, Payment> byRide = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        byRide.put(payment.rideId(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findByRideId(UUID rideId) {
        return Optional.ofNullable(byRide.get(rideId));
    }

    @Override
    public boolean existsByRideId(UUID rideId) {
        return byRide.containsKey(rideId);
    }
}
