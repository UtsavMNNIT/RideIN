package com.rideflow.trip.application.usecase;

import com.rideflow.trip.application.port.out.TripRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sweeps offers whose TTL has lapsed and expires them (the ride fails — no
 * re-dispatch this iteration). Pure orchestration: it queries timed-out offer
 * ids, then drives each through {@link TripActionService#expire} — a separate
 * bean, so the {@code @Transactional} + {@code @Version} guard actually applies.
 *
 * <p><b>Expiry-vs-accept race:</b> if a driver accepts at the same instant, both
 * paths reload the same version; one commits and the other gets an
 * {@link OptimisticLockingFailureException}. The sweeper simply skips the loser
 * — the driver won, the trip is ACCEPTED, no double terminal event.
 */
@Component
public class ExpireOffersService {

    private static final Logger log = LoggerFactory.getLogger(ExpireOffersService.class);
    private static final int BATCH = 100;

    private final TripRepository    trips;
    private final TripActionService actions;

    public ExpireOffersService(TripRepository trips, TripActionService actions) {
        this.trips   = trips;
        this.actions = actions;
    }

    @Scheduled(fixedDelayString = "${rideflow.trip.sweeper.interval-ms}")
    public void sweep() {
        List<UUID> expired = trips.findExpiredOfferIds(Instant.now(), BATCH);
        if (expired.isEmpty()) {
            return;
        }
        int swept = 0;
        for (UUID rideId : expired) {
            try {
                actions.expire(rideId);
                swept++;
            } catch (OptimisticLockingFailureException e) {
                log.debug("Skipping offer {} — driver responded first", rideId);
            } catch (Exception e) {
                // A single bad row must not stall the sweep; log and continue.
                log.warn("Failed to expire offer {}: {}", rideId, e.getMessage());
            }
        }
        if (swept > 0) {
            log.info("Offer sweeper expired {} offer(s)", swept);
        }
    }
}
