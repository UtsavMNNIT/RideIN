package com.rideflow.pricing.infrastructure.persistence.jpa;

import com.rideflow.pricing.application.port.out.QuoteRepository;
import com.rideflow.pricing.domain.model.FareBreakdown;
import com.rideflow.pricing.domain.model.GeoPoint;
import com.rideflow.pricing.domain.model.Money;
import com.rideflow.pricing.domain.model.Quote;
import com.rideflow.pricing.domain.model.TripEstimate;
import com.rideflow.pricing.infrastructure.persistence.jpa.entity.QuoteEntity;
import com.rideflow.pricing.infrastructure.persistence.jpa.repository.QuoteJpaRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** JPA adapter for {@link QuoteRepository} — maps {@link Quote} ↔ {@link QuoteEntity}. */
@Repository
public class JpaQuoteRepository implements QuoteRepository {

    private final QuoteJpaRepository jpa;

    public JpaQuoteRepository(QuoteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Quote save(Quote quote) {
        return toDomain(jpa.save(toEntity(quote)));
    }

    @Override
    public Optional<Quote> findById(UUID quoteId) {
        return jpa.findById(quoteId).map(JpaQuoteRepository::toDomain);
    }

    @Override
    public Optional<Quote> findByRideId(UUID rideId) {
        return jpa.findByRideId(rideId).map(JpaQuoteRepository::toDomain);
    }

    @Override
    public boolean existsByRideId(UUID rideId) {
        return jpa.existsByRideId(rideId);
    }

    private static QuoteEntity toEntity(Quote q) {
        FareBreakdown f = q.fare();
        return new QuoteEntity(
                q.id(), q.rideId(), q.riderId(),
                q.pickup().lat(), q.pickup().lng(),
                q.dropoff().lat(), q.dropoff().lng(),
                q.vehicleType(), f.currency(),
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
                q.validUntil(), q.createdAt());
    }

    private static Quote toDomain(QuoteEntity e) {
        String ccy = e.getCurrency();
        FareBreakdown fare = new FareBreakdown(
                Money.of(e.getBaseFare(), ccy),
                Money.of(e.getDistanceFare(), ccy),
                Money.of(e.getTimeFare(), ccy),
                Money.of(e.getSubtotal(), ccy),
                e.getSurgeMultiplier(),
                Money.of(e.getSurgedSubtotal(), ccy),
                Money.of(e.getBookingFee(), ccy),
                Money.of(e.getTotal(), ccy));

        TripEstimate estimate = new TripEstimate(e.getEstDistanceKm(), e.getEstDurationMin());

        return new Quote(
                e.getId(), e.getRideId(), e.getRiderId(),
                new GeoPoint(e.getPickupLat(), e.getPickupLng()),
                new GeoPoint(e.getDropoffLat(), e.getDropoffLng()),
                e.getVehicleType(), estimate, fare,
                e.getValidUntil(), e.getCreatedAt());
    }
}
