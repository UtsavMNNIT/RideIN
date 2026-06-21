package com.rideflow.pricing.application.usecase;

import com.rideflow.pricing.application.port.out.QuoteRepository;
import com.rideflow.pricing.application.port.out.RateCardProvider;
import com.rideflow.pricing.application.port.out.SurgeProvider;
import com.rideflow.pricing.application.port.out.TripEstimator;
import com.rideflow.pricing.domain.model.GeoPoint;
import com.rideflow.pricing.domain.model.Money;
import com.rideflow.pricing.domain.model.Quote;
import com.rideflow.pricing.domain.model.RateCard;
import com.rideflow.pricing.domain.model.TripEstimate;
import com.rideflow.pricing.domain.model.VehicleType;
import com.rideflow.pricing.domain.service.FareCalculator;
import com.rideflow.pricing.infrastructure.config.PricingProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Happy-path unit test for the up-front quote use case with hand-rolled fakes. */
class QuoteFareUseCaseTest {

    private static final String CCY = "INR";
    private static final GeoPoint PICKUP = new GeoPoint(12.97, 77.59);
    private static final GeoPoint DROPOFF = new GeoPoint(13.00, 77.62);

    private static Money inr(String v) { return Money.of(new BigDecimal(v), CCY); }

    private static RateCard card() {
        return new RateCard(VehicleType.STANDARD,
                inr("50.00"), inr("10.00"), inr("2.00"), inr("80.00"), inr("15.00"));
    }

    // ---- fakes ------------------------------------------------------------

    private static final class FixedEstimator implements TripEstimator {
        private final TripEstimate estimate;
        FixedEstimator(TripEstimate e) { this.estimate = e; }
        @Override public TripEstimate estimate(GeoPoint p, GeoPoint d, VehicleType v) { return estimate; }
    }

    private static final class FixedRateCards implements RateCardProvider {
        private final RateCard card;
        FixedRateCards(RateCard c) { this.card = c; }
        @Override public RateCard cardFor(VehicleType vehicleType) { return card; }
        @Override public List<RateCard> all() { return List.of(card); }
    }

    private static final class FixedSurge implements SurgeProvider {
        private final BigDecimal multiplier;
        FixedSurge(BigDecimal m) { this.multiplier = m; }
        @Override public BigDecimal multiplierFor(VehicleType v, GeoPoint pickup) { return multiplier; }
    }

    private static final class InMemoryQuotes implements QuoteRepository {
        final List<Quote> saved = new ArrayList<>();
        @Override public Quote save(Quote quote) { saved.add(quote); return quote; }
        @Override public Optional<Quote> findById(UUID id) { return Optional.empty(); }
        @Override public Optional<Quote> findByRideId(UUID rideId) { return Optional.empty(); }
        @Override public boolean existsByRideId(UUID rideId) { return false; }
    }

    private static PricingProperties props() {
        return new PricingProperties(CCY,
                new PricingProperties.Estimate(1.3, 30.0),
                new PricingProperties.Quote(5),
                new PricingProperties.Surge(new BigDecimal("1.00"), Map.of(), null));
    }

    @Test
    void happyPath_pricesEstimateAndPersistsQuote() {
        InMemoryQuotes repo = new InMemoryQuotes();
        // 10 km, 20 min => subtotal 50 + 100 + 40 = 190; surge 2.0 => 380; + 15 = 395
        QuoteFareUseCase useCase = new QuoteFareUseCase(
                new FixedEstimator(new TripEstimate(10.0, 20.0)),
                new FixedRateCards(card()),
                new FixedSurge(new BigDecimal("2.0")),
                new FareCalculator(),
                repo,
                props());

        UUID riderId = UUID.randomUUID();
        Quote q = useCase.quote(new QuoteFareUseCase.QuoteCommand(
                riderId, PICKUP, DROPOFF, VehicleType.STANDARD));

        // Returned quote is the persisted one.
        assertThat(repo.saved).hasSize(1).containsExactly(q);

        assertThat(q.id()).isNotNull();
        assertThat(q.rideId()).isNull();              // up-front quote
        assertThat(q.riderId()).isEqualTo(riderId);
        assertThat(q.pickup()).isEqualTo(PICKUP);
        assertThat(q.dropoff()).isEqualTo(DROPOFF);
        assertThat(q.vehicleType()).isEqualTo(VehicleType.STANDARD);
        assertThat(q.estimate().distanceKm()).isEqualTo(10.0);

        assertThat(q.fare().subtotal()).isEqualTo(inr("190.00"));
        assertThat(q.fare().surgedSubtotal()).isEqualTo(inr("380.00"));
        assertThat(q.fare().total()).isEqualTo(inr("395.00"));

        // TTL of 5 minutes => validUntil after createdAt by 5 min.
        assertThat(q.validUntil()).isEqualTo(q.createdAt().plusSeconds(300));
    }

    @Test
    void anonymousQuote_allowsNullRider() {
        InMemoryQuotes repo = new InMemoryQuotes();
        QuoteFareUseCase useCase = new QuoteFareUseCase(
                new FixedEstimator(new TripEstimate(2.0, 5.0)),
                new FixedRateCards(card()),
                new FixedSurge(BigDecimal.ONE),
                new FareCalculator(),
                repo,
                props());

        Quote q = useCase.quote(new QuoteFareUseCase.QuoteCommand(
                null, PICKUP, DROPOFF, VehicleType.STANDARD));

        assertThat(q.riderId()).isNull();
        assertThat(repo.saved).hasSize(1);
    }
}
