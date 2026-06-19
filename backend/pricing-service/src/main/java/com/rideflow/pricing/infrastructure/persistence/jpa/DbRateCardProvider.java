package com.rideflow.pricing.infrastructure.persistence.jpa;

import com.rideflow.pricing.application.port.out.RateCardProvider;
import com.rideflow.pricing.domain.exception.RateCardNotFoundException;
import com.rideflow.pricing.domain.model.Money;
import com.rideflow.pricing.domain.model.RateCard;
import com.rideflow.pricing.domain.model.VehicleType;
import com.rideflow.pricing.infrastructure.persistence.jpa.entity.RateCardEntity;
import com.rideflow.pricing.infrastructure.persistence.jpa.repository.RateCardJpaRepository;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * {@link RateCardProvider} backed by {@code pricing.rate_cards}, cached in memory
 * so pricing a fare never touches the DB on the hot path.
 *
 * <p>The cache is loaded at startup and refreshed on a fixed interval
 * ({@code rideflow.pricing.rate-card.refresh-ms}); an operator's rate change
 * takes effect within one refresh window with no redeploy. The map reference is
 * swapped atomically, so readers always see a consistent snapshot.
 */
@Component
public class DbRateCardProvider implements RateCardProvider {

    private static final Logger log = LoggerFactory.getLogger(DbRateCardProvider.class);

    private final RateCardJpaRepository repo;
    private volatile Map<VehicleType, RateCard> cache = new ConcurrentHashMap<>();

    public DbRateCardProvider(RateCardJpaRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    void loadOnStartup() {
        refresh();
        if (cache.isEmpty()) {
            log.warn("No rate cards found at startup — pricing will fail until cards are seeded");
        }
    }

    @Scheduled(fixedDelayString = "${rideflow.pricing.rate-card.refresh-ms:60000}")
    void refresh() {
        Map<VehicleType, RateCard> fresh = repo.findAll().stream()
                .collect(Collectors.toMap(RateCardEntity::getVehicleType, DbRateCardProvider::toDomain));
        this.cache = fresh;
        log.debug("Rate-card cache refreshed: {} card(s)", fresh.size());
    }

    @Override
    public RateCard cardFor(VehicleType vehicleType) {
        RateCard card = cache.get(vehicleType);
        if (card == null) {
            // Cache miss can mean a card added since the last refresh — try once live.
            card = repo.findById(vehicleType).map(DbRateCardProvider::toDomain).orElse(null);
            if (card == null) {
                throw new RateCardNotFoundException(vehicleType);
            }
        }
        return card;
    }

    @Override
    public List<RateCard> all() {
        return List.copyOf(cache.values());
    }

    private static RateCard toDomain(RateCardEntity e) {
        String ccy = e.getCurrency();
        return new RateCard(
                e.getVehicleType(),
                Money.of(e.getBaseFare(), ccy),
                Money.of(e.getPerKm(), ccy),
                Money.of(e.getPerMinute(), ccy),
                Money.of(e.getMinimumFare(), ccy),
                Money.of(e.getBookingFee(), ccy));
    }
}
