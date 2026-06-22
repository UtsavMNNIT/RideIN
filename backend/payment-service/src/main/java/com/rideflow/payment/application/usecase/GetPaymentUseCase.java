package com.rideflow.payment.application.usecase;

import com.rideflow.payment.application.port.out.PaymentRepository;
import com.rideflow.payment.domain.exception.PaymentNotFoundException;
import com.rideflow.payment.domain.model.Payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Fetch the settlement/receipt for a ride. */
@Service
public class GetPaymentUseCase {

    private final PaymentRepository payments;

    public GetPaymentUseCase(PaymentRepository payments) {
        this.payments = payments;
    }

    @Transactional(readOnly = true)
    public Payment byRideId(UUID rideId) {
        return payments.findByRideId(rideId).orElseThrow(() -> new PaymentNotFoundException(rideId));
    }
}
