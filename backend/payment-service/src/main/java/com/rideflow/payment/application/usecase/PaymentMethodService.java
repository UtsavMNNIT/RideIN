package com.rideflow.payment.application.usecase;

import com.rideflow.payment.application.port.out.PaymentMethodRepository;
import com.rideflow.payment.domain.model.PaymentMethod;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Add/list mock saved cards. No real PSP — purely a demo of the flow. */
@Service
public class PaymentMethodService {

    private final PaymentMethodRepository methods;

    public PaymentMethodService(PaymentMethodRepository methods) {
        this.methods = methods;
    }

    @Transactional
    public PaymentMethod add(UUID userId, String brand, String last4, boolean isDefault) {
        return methods.save(PaymentMethod.create(userId, brand, last4, isDefault));
    }

    @Transactional(readOnly = true)
    public List<PaymentMethod> list(UUID userId) {
        return methods.findByUser(userId);
    }
}
