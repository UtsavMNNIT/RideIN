package com.rideflow.location.domain.repository;

import com.rideflow.location.domain.model.DriverLocation;
import com.rideflow.location.domain.model.NearbyDriver;
import com.rideflow.location.domain.model.NearbyQuery;

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
}
