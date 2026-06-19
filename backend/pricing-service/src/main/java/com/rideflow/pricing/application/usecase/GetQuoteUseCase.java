package com.rideflow.pricing.application.usecase;

import com.rideflow.pricing.application.port.out.QuoteRepository;
import com.rideflow.pricing.domain.exception.QuoteNotFoundException;
import com.rideflow.pricing.domain.model.Quote;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Retrieves a previously computed quote by id (for the rider app / receipts). */
@Service
public class GetQuoteUseCase {

    private final QuoteRepository quoteRepository;

    public GetQuoteUseCase(QuoteRepository quoteRepository) {
        this.quoteRepository = quoteRepository;
    }

    @Transactional(readOnly = true)
    public Quote byId(UUID quoteId) {
        return quoteRepository.findById(quoteId)
                .orElseThrow(() -> new QuoteNotFoundException(quoteId));
    }
}
