package com.rideflow.payment.application.port.out;

import com.rideflow.payment.domain.model.PaymentMethod;

import java.util.List;
import java.util.UUID;

/** Output port for mock saved payment methods. */
public interface PaymentMethodRepository {

    PaymentMethod save(PaymentMethod method);

    List<PaymentMethod> findByUser(UUID userId);
}
