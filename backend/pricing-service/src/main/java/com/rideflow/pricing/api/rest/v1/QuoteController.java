package com.rideflow.pricing.api.rest.v1;

import com.rideflow.pricing.api.dto.request.QuoteRequest;
import com.rideflow.pricing.api.dto.response.QuoteResponse;
import com.rideflow.pricing.application.usecase.GetQuoteUseCase;
import com.rideflow.pricing.application.usecase.QuoteFareUseCase;
import com.rideflow.pricing.domain.model.GeoPoint;
import com.rideflow.pricing.domain.model.Quote;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Up-front quote API.
 *
 * <pre>
 *   POST /v1/quotes              compute an up-front fare estimate
 *   GET  /v1/quotes/{quoteId}    fetch a previously computed quote
 * </pre>
 *
 * <p>Ride fares are computed asynchronously off {@code rider.RideRequested} and
 * are not created through this controller; they are readable here by their
 * {@code quoteId} once produced.
 */
@RestController
@RequestMapping("/v1/quotes")
public class QuoteController {

    private final QuoteFareUseCase quoteFare;
    private final GetQuoteUseCase  getQuote;

    public QuoteController(QuoteFareUseCase quoteFare, GetQuoteUseCase getQuote) {
        this.quoteFare = quoteFare;
        this.getQuote  = getQuote;
    }

    @PostMapping
    public ResponseEntity<QuoteResponse> create(@Valid @RequestBody QuoteRequest req) {
        Quote quote = quoteFare.quote(new QuoteFareUseCase.QuoteCommand(
                req.riderId(),
                new GeoPoint(req.pickupLat(), req.pickupLng()),
                new GeoPoint(req.dropoffLat(), req.dropoffLng()),
                req.vehicleType()));

        return ResponseEntity
                .created(URI.create("/v1/quotes/" + quote.id()))
                .body(QuoteResponse.from(quote));
    }

    @GetMapping("/{quoteId}")
    public QuoteResponse get(@PathVariable UUID quoteId) {
        return QuoteResponse.from(getQuote.byId(quoteId));
    }
}
