package com.rideflow.location.domain.repository;

import com.rideflow.location.domain.model.DriverLocation;
import com.rideflow.location.domain.model.NearbyDriver;
import com.rideflow.location.domain.model.NearbyQuery;

import java.time.Duration;
import java.util.List;

/**
 * Outbound port for driver-location persistence. The Redis-Geo adapter
 * lives in {@code infrastructure.cache.redis}.
 *
 * Implementations MUST be atomic on a per-driver basis — partial updates
 * (e.g. metadata written but geo index missed) would silently corrupt
 * dispatch decisions.
 */
public interface DriverLocationRepository {

    /**
     * Upsert a driver's location.
     *
     * Contract:
     *  - Idempotent: replays with same or older {@code capturedAt} are no-ops.
     *  - Atomic: geo index, metadata hash, and heartbeat set move together.
     *  - Migrates the driver between the available and busy shards if
     *    {@code availability} changed.
     *
     * @return {@code true} if applied, {@code false} if dropped as stale.
     */
    boolean upsert(DriverLocation location);

    /**
     * Find up to {@code query.limit()} drivers within {@code query.radiusMeters()}
     * of {@code (query.lat(), query.lng())}, restricted to {@code query.vehicleType()},
     * ordered nearest-first.
     */
    List<NearbyDriver> findNearby(NearbyQuery query);

    /**
     * Evict drivers whose most recent heartbeat is older than {@code olderThan}
     * from the geo index, metadata, and heartbeat set.
     *
     * <p>Without this, a driver that crashes or loses connectivity (and so stops
     * emitting location events) would linger in the geo index indefinitely —
     * {@code GEOADD} members never expire on their own — and be returned by
     * {@link #findNearby} as a "ghost" candidate.
     *
     * <p>Implementations MUST be safe under concurrent invocation from multiple
     * pods: a driver that refreshes its heartbeat between the scan and the
     * removal must NOT be evicted (compare-and-remove on the heartbeat score).
     *
     * @return the number of drivers actually evicted.
     */
    int evictStale(Duration olderThan);
}
