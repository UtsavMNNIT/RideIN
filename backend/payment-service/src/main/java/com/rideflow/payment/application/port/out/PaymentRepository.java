package com.rideflow.payment.application.port.out;

import com.rideflow.payment.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

/** Output port for persisting and querying {@link Payment} aggregates. */
public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findByRideId(UUID rideId);

    boolean existsByRideId(UUID rideId);
}
