package com.rideflow.location.application.scheduler;

import com.rideflow.location.support.InMemoryDriverLocationRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class StaleDriverSweeperTest {

    @Test
    void sweep_evictsStaleDrivers_andCountsThem() {
        InMemoryDriverLocationRepository repository = new InMemoryDriverLocationRepository();
        repository.evictStaleResult = 3;
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        StaleDriverSweeper sweeper = new StaleDriverSweeper(
                repository, Duration.ofSeconds(30), registry);

        sweeper.sweep();

        assertThat(registry.get("rideflow.location.stale_evicted").counter().count()).isEqualTo(3.0);
    }

    @Test
    void sweep_zeroEvicted_doesNotIncrementCounter() {
        InMemoryDriverLocationRepository repository = new InMemoryDriverLocationRepository();
        repository.evictStaleResult = 0;
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        StaleDriverSweeper sweeper = new StaleDriverSweeper(
                repository, Duration.ofSeconds(30), registry);

        sweeper.sweep();

        assertThat(registry.get("rideflow.location.stale_evicted").counter().count()).isZero();
    }

    @Test
    void sweep_swallowsRepositoryFailure() {
        AtomicInteger calls = new AtomicInteger();
        // Anonymous subclass overriding evictStale to throw.
        InMemoryDriverLocationRepository repository = new InMemoryDriverLocationRepository() {
            @Override
            public int evictStale(Duration olderThan) {
                calls.incrementAndGet();
                throw new RuntimeException("redis down");
            }
        };
        StaleDriverSweeper sweeper = new StaleDriverSweeper(
                repository, Duration.ofSeconds(30), new SimpleMeterRegistry());

        // Must not propagate — scheduler thread stays alive.
        sweeper.sweep();

        assertThat(calls.get()).isEqualTo(1);
    }
}
