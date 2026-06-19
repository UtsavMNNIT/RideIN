package com.rideflow.pricing.application.usecase;

import com.rideflow.pricing.application.port.out.FareEventPublisher;
import com.rideflow.pricing.application.port.out.ProcessedEventStore;
import com.rideflow.pricing.application.port.out.QuoteRepository;
import com.rideflow.pricing.application.port.out.RateCardProvider;
import com.rideflow.pricing.application.port.out.SurgeProvider;
import com.rideflow.pricing.application.port.out.TripEstimator;
import com.rideflow.pricing.domain.event.FareQuoted;
import com.rideflow.pricing.domain.model.FareBreakdown;
import com.rideflow.pricing.domain.model.GeoPoint;
import com.rideflow.pricing.domain.model.Quote;
import com.rideflow.pricing.domain.model.RateCard;
import com.rideflow.pricing.domain.model.TripEstimate;
import com.rideflow.pricing.domain.model.VehicleType;
import com.rideflow.pricing.domain.service.FareCalculator;
import com.rideflow.pricing.infrastructure.config.PricingProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Prices an actual ride in response to {@code rider.RideRequested}, and publishes
 * the authoritative fare as {@code pricing.fare-quoted}.
 *
 * <h3>Exactly-once effect over at-least-once delivery</h3>
 * The whole operation is one transaction with two layers of idempotency:
 * <ol>
 *   <li>the {@link ProcessedEventStore} — a redelivered envelope (same
 *       {@code eventId}) short-circuits before any work;</li>
 *   <li>{@code existsByRideId} + the DB's unique index on {@code quotes.ride_id}
 *       — a different envelope for an already-priced ride still yields exactly
 *       one quote.</li>
 * </ol>
 * The quote row, the outbox row, and the processed-event marker all commit
 * together — or none do. If the transaction rolls back, the Kafka offset is not
 * acked and the message is safely redelivered.
 */
@Service
public class PriceRideUseCase {

    private static final Logger log = LoggerFactory.getLogger(PriceRideUseCase.class);

    private final TripEstimator       tripEstimator;
    private final RateCardProvider    rateCardProvider;
    private final SurgeProvider       surgeProvider;
    private final FareCalculator      fareCalculator;
    private final QuoteRepository     quoteRepository;
    private final FareEventPublisher  fareEventPublisher;
    private final ProcessedEventStore processedEventStore;
    private final String              consumerGroup;
    private final Duration            quoteTtl;

    public PriceRideUseCase(TripEstimator tripEstimator,
                            RateCardProvider rateCardProvider,
                            SurgeProvider surgeProvider,
                            FareCalculator fareCalculator,
                            QuoteRepository quoteRepository,
                            FareEventPublisher fareEventPublisher,
                            ProcessedEventStore processedEventStore,
                            PricingProperties properties,
                            @org.springframework.beans.factory.annotation.Value("${spring.kafka.consumer.group-id}")
                            String consumerGroup) {
        this.tripEstimator       = tripEstimator;
        this.rateCardProvider    = rateCardProvider;
        this.surgeProvider       = surgeProvider;
        this.fareCalculator      = fareCalculator;
        this.quoteRepository     = quoteRepository;
        this.fareEventPublisher  = fareEventPublisher;
        this.processedEventStore = processedEventStore;
        this.consumerGroup       = consumerGroup;
        this.quoteTtl            = Duration.ofMinutes(properties.quote().ttlMinutes());
    }

    @Transactional
    public void price(PriceRideCommand cmd) {
        if (processedEventStore.isProcessed(cmd.eventId(), consumerGroup)) {
            log.debug("Skipping already-processed eventId={}", cmd.eventId());
            return;
        }
        if (quoteRepository.existsByRideId(cmd.rideId())) {
            // Ride already priced under a different envelope — record this event
            // as handled and stop. Keeps "one quote per ride" without erroring.
            log.debug("Ride {} already priced; marking event {} processed", cmd.rideId(), cmd.eventId());
            processedEventStore.markProcessed(cmd.eventId(), consumerGroup);
            return;
        }

        TripEstimate  estimate = tripEstimator.estimate(cmd.pickup(), cmd.dropoff(), cmd.vehicleType());
        RateCard      card     = rateCardProvider.cardFor(cmd.vehicleType());
        BigDecimal    surge    = surgeProvider.multiplierFor(cmd.vehicleType(), cmd.pickup());
        FareBreakdown fare     = fareCalculator.price(card, estimate, surge);

        Instant now        = Instant.now();
        Instant validUntil = now.plus(quoteTtl);

        Quote quote = Quote.forRide(
                UUID.randomUUID(), cmd.rideId(), cmd.riderId(),
                cmd.pickup(), cmd.dropoff(), cmd.vehicleType(),
                estimate, fare, now, validUntil);
        quoteRepository.save(quote);

        fareEventPublisher.publishFareQuoted(toEvent(quote));
        processedEventStore.markProcessed(cmd.eventId(), consumerGroup);

        log.info("Priced ride {} → {} {} (surge {})",
                cmd.rideId(), fare.total().amount(), fare.currency(), surge);
    }

    private FareQuoted toEvent(Quote q) {
        FareBreakdown f = q.fare();
        return new FareQuoted(
                q.id(),
                q.rideId(),
                q.riderId(),
                q.vehicleType().name(),
                f.currency(),
                f.baseFare().amount(),
                f.distanceFare().amount(),
                f.timeFare().amount(),
                f.subtotal().amount(),
                f.surgeMultiplier(),
                f.surgedSubtotal().amount(),
                f.bookingFee().amount(),
                f.total().amount(),
                q.estimate().distanceKm(),
                q.estimate().durationMinutes(),
                q.validUntil(),
                q.createdAt());
    }

    /** Inputs derived from a {@code rider.RideRequested} envelope. */
    public record PriceRideCommand(
            UUID        eventId,
            UUID        rideId,
            UUID        riderId,
            GeoPoint    pickup,
            GeoPoint    dropoff,
            VehicleType vehicleType,
            Instant     requestedAt) {
    }
}
