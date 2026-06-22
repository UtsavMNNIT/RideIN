package com.rideflow.payment.domain.exception;

import com.rideflow.payment.domain.model.PaymentStatus;

/** An action was attempted from a state that does not allow it. Maps to HTTP 409. */
public class IllegalPaymentTransitionException extends PaymentException {

    public IllegalPaymentTransitionException(String action, PaymentStatus from) {
        super("cannot " + action + " a payment in state " + from);
    }
}
