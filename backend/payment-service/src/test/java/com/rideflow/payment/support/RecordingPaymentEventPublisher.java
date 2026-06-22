package com.rideflow.payment.support;

import com.rideflow.payment.application.port.out.PaymentEventPublisher;
import com.rideflow.payment.domain.event.PaymentSettled;

import java.util.ArrayList;
import java.util.List;

/** Records published events for assertions. */
public class RecordingPaymentEventPublisher implements PaymentEventPublisher {

    public final List<PaymentSettled> settled = new ArrayList<>();

    @Override
    public void publishPaymentSettled(PaymentSettled event) {
        settled.add(event);
    }
}
