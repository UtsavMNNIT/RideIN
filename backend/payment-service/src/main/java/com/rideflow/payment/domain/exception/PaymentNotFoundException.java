package com.rideflow.payment.domain.exception;

import java.util.UUID;

/** No payment exists for a given ride id. */
public class PaymentNotFoundException extends PaymentException {

    public PaymentNotFoundException(UUID rideId) {
        super("payment not found for ride: " + rideId);
    }
}
