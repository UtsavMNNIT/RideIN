package com.rideflow.payment.application.port.out;

import com.rideflow.payment.domain.event.PaymentSettled;

/** Output port for emitting payment domain events to the outside world. */
public interface PaymentEventPublisher {

    void publishPaymentSettled(PaymentSettled event);
}
