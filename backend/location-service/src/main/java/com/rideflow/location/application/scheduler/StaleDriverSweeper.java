package com.rideflow.location.application.scheduler;

import com.rideflow.location.domain.repository.DriverLocationRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Periodically evicts drivers whose heartbeat has gone stale from the geo index.
 *
 * <p><b>Why this exists:</b> location events only arrive while a driver is
 * actively pinging. A driver who crashes, force-quits the app, or loses
 * connectivity simply stops emitting — but {@code GEOADD} members never expire,
 * so without an active sweep the driver stays in the available geo set and gets
 * returned by proximity queries as a "ghost". The sweeper bounds that staleness
 * to {@code stale-after}.
 *
 * <p><b>Multi-pod safety:</b> every pod runs this schedule, but the repository's
 * eviction is a per-driver compare-and-remove on the heartbeat score, so
 * concurrent sweeps are idempotent (a double-removal is a no-op, and a driver
 * who refreshed is never evicted). Redundant scans are cheap; if they ever
 * matter, gate this behind a Redis leader lock.
 */
@Component
public class StaleDriverSweeper {

    private static final Logger log = LoggerFactory.getLogger(StaleDriverSweeper.class);

    private final DriverLocationRepository repository;
    private final Duration                 staleAfter;
    private final Counter                  evictedCounter;

    public StaleDriverSweeper(
            DriverLocationRepository repository,
            @Value("${rideflow.location.sweeper.stale-after}") Duration staleAfter,
            MeterRegistry registry) {
        this.repository     = repository;
        this.staleAfter     = staleAfter;
        this.evictedCounter = Counter.builder("rideflow.location.stale_evicted")
                .description("Drivers removed from the geo index by the stale sweeper")
                .register(registry);
    }

    @Scheduled(fixedDelayString = "${rideflow.location.sweeper.interval-ms}")
    public void sweep() {
        try {
            int evicted = repository.evictStale(staleAfter);
            if (evicted > 0) {
                evictedCounter.increment(evicted);
            }
        } catch (RuntimeException e) {
            // Never let a transient Redis hiccup kill the scheduler thread.
            log.warn("Stale-driver sweep failed; will retry next interval", e);
        }
    }
}
