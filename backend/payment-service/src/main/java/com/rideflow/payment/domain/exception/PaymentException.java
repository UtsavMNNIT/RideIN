package com.rideflow.payment.domain.exception;

/** Base type for all payment-domain failures. */
public abstract class PaymentException extends RuntimeException {

    protected PaymentException(String message) {
        super(message);
    }
}
