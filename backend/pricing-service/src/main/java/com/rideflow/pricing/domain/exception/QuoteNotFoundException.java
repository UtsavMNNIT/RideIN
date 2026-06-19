package com.rideflow.pricing.domain.exception;

import java.util.UUID;

/** A requested quote does not exist. */
public class QuoteNotFoundException extends PricingException {

    public QuoteNotFoundException(UUID quoteId) {
        super("quote not found: " + quoteId);
    }
}
