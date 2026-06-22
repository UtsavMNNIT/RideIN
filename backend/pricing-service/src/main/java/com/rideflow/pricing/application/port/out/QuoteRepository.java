package com.rideflow.pricing.application.port.out;

import com.rideflow.pricing.domain.model.Quote;

import java.util.Optional;
import java.util.UUID;

/** Output port: persists and looks up {@link Quote}s. */
public interface QuoteRepository {

    Quote save(Quote quote);

    Optional<Quote> findById(UUID quoteId);

    Optional<Quote> findByRideId(UUID rideId);

    boolean existsByRideId(UUID rideId);
}
