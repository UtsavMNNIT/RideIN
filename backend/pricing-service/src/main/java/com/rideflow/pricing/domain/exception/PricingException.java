package com.rideflow.pricing.domain.exception;

/** Base type for all pricing-domain failures. */
public abstract class PricingException extends RuntimeException {

    protected PricingException(String message) {
        super(message);
    }
}
