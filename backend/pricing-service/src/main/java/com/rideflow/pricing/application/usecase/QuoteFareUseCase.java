package com.rideflow.pricing.application.usecase;

import com.rideflow.pricing.application.port.out.QuoteRepository;
import com.rideflow.pricing.application.port.out.RateCardProvider;
import com.rideflow.pricing.application.port.out.SurgeProvider;
import com.rideflow.pricing.application.port.out.TripEstimator;
import com.rideflow.pricing.domain.model.FareBreakdown;
import com.rideflow.pricing.domain.model.GeoPoint;
import com.rideflow.pricing.domain.model.Quote;
import com.rideflow.pricing.domain.model.RateCard;
import com.rideflow.pricing.domain.model.TripEstimate;
import com.rideflow.pricing.domain.model.VehicleType;
import com.rideflow.pricing.domain.service.FareCalculator;
import com.rideflow.pricing.infrastructure.config.PricingProperties;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Up-front fare quote: "how much would this trip cost right now?"
 *
 * <p>Synchronous, request/response. Estimates the trip, looks up the rate card
 * and the surge in effect, runs the {@link FareCalculator}, and persists the
 * quote with a TTL so the same price can be honoured for a short window if the
 * rider then books. No event is published — an up-front quote is not a ride.
 */
@Service
public class QuoteFareUseCase {

    private final TripEstimator    tripEstimator;
    private final RateCardProvider rateCardProvider;
    private final SurgeProvider    surgeProvider;
    private final FareCalculator   fareCalculator;
    private final QuoteRepository  quoteRepository;
    private final Duration         quoteTtl;

    public QuoteFareUseCase(TripEstimator tripEstimator,
                            RateCardProvider rateCardProvider,
                            SurgeProvider surgeProvider,
                            FareCalculator fareCalculator,
                            QuoteRepository quoteRepository,
                            PricingProperties properties) {
        this.tripEstimator    = tripEstimator;
        this.rateCardProvider = rateCardProvider;
        this.surgeProvider    = surgeProvider;
        this.fareCalculator   = fareCalculator;
        this.quoteRepository  = quoteRepository;
        this.quoteTtl         = Duration.ofMinutes(properties.quote().ttlMinutes());
    }

    @Transactional
    public Quote quote(QuoteCommand cmd) {
        TripEstimate  estimate = tripEstimator.estimate(cmd.pickup(), cmd.dropoff(), cmd.vehicleType());
        RateCard      card     = rateCardProvider.cardFor(cmd.vehicleType());
        BigDecimal    surge    = surgeProvider.multiplierFor(cmd.vehicleType(), cmd.pickup());
        FareBreakdown fare     = fareCalculator.price(card, estimate, surge);

        Instant now        = Instant.now();
        Instant validUntil = now.plus(quoteTtl);

        Quote quote = Quote.upfront(
                UUID.randomUUID(), cmd.riderId(),
                cmd.pickup(), cmd.dropoff(), cmd.vehicleType(),
                estimate, fare, now, validUntil);

        return quoteRepository.save(quote);
    }

    /** Inputs to an up-front quote. {@code riderId} is optional (anonymous quotes allowed). */
    public record QuoteCommand(
            UUID        riderId,
            GeoPoint    pickup,
            GeoPoint    dropoff,
            VehicleType vehicleType) {
    }
}
