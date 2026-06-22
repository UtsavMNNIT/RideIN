package com.rideflow.location.infrastructure.cache.redis;

import com.rideflow.location.domain.model.DriverLocation;
import com.rideflow.location.domain.model.NearbyDriver;
import com.rideflow.location.domain.model.NearbyQuery;
import com.rideflow.location.domain.model.VehicleType;
import com.rideflow.location.domain.repository.DriverLocationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Range;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.DistanceUnit;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.GeoShape;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Redis-Geo adapter for {@link DriverLocationRepository}.
 *
 * <p>Writes go through a Lua script for atomicity. Reads use {@code GEOSEARCH}
 * directly — one command, sub-millisecond.
 */
@Repository
public class RedisDriverLocationRepository implements DriverLocationRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisDriverLocationRepository.class);

    private static final String GEO_AVAILABLE_PREFIX = "geo:drivers:available:";
    private static final String GEO_BUSY_PREFIX      = "geo:drivers:busy:";
    private static final String META_PREFIX          = "driver:meta:";
    private static final String HEARTBEAT_KEY        = "driver:heartbeat";

    /** Metadata TTL: a few heartbeats of grace before sweeping. */
    private static final long META_TTL_SECONDS = 60;

    private final StringRedisTemplate         redis;
    private final DefaultRedisScript<Long>    updateScript;
    private final DefaultRedisScript<Long>    evictScript;
    private final DefaultRedisScript<Long>    removeScript;
    private final DefaultRedisScript<Long>    markBusyScript;

    /** Every geo shard key (available + busy for each vehicle type), computed once. */
    private final List<String> allGeoKeys;

    public RedisDriverLocationRepository(
            @Qualifier("geoRedisTemplate") StringRedisTemplate redis,
            DefaultRedisScript<Long> updateDriverLocationScript,
            DefaultRedisScript<Long> evictStaleDriverScript,
            DefaultRedisScript<Long> removeDriverScript,
            DefaultRedisScript<Long> markDriverBusyScript) {
        this.redis          = redis;
        this.updateScript   = updateDriverLocationScript;
        this.evictScript    = evictStaleDriverScript;
        this.removeScript   = removeDriverScript;
        this.markBusyScript = markDriverBusyScript;

        List<String> geoKeys = new ArrayList<>(VehicleType.values().length * 2);
        for (VehicleType vt : VehicleType.values()) {
            geoKeys.add(GEO_AVAILABLE_PREFIX + vt.name());
            geoKeys.add(GEO_BUSY_PREFIX + vt.name());
        }
        this.allGeoKeys = List.copyOf(geoKeys);
    }

    // -------------------------------------------------------------------
    // Write path
    // -------------------------------------------------------------------

    @Override
    public boolean upsert(DriverLocation loc) {
        String vt          = loc.vehicleType().name();
        String availableK  = GEO_AVAILABLE_PREFIX + vt;
        String busyK       = GEO_BUSY_PREFIX + vt;
        String metaK       = META_PREFIX + loc.driverId();

        List<String> keys = List.of(availableK, busyK, metaK, HEARTBEAT_KEY);

        Object[] args = new Object[] {
                loc.driverId().toString(),
                Double.toString(loc.lng()),
                Double.toString(loc.lat()),
                loc.headingDegrees() == null ? "" : loc.headingDegrees().toString(),
                loc.speedMps()       == null ? "" : loc.speedMps().toString(),
                loc.availability().name(),
                vt,
                Long.toString(loc.capturedAt().toEpochMilli()),
                Long.toString(META_TTL_SECONDS)
        };

        Long result = redis.execute(updateScript, keys, args);

        if (result == null) {
            log.warn("Lua returned null for driverId={}", loc.driverId());
            return false;
        }
        if (result == 0L) {
            log.debug("Stale location dropped driverId={} capturedAt={}",
                    loc.driverId(), loc.capturedAt());
            return false;
        }
        return true;
    }

    // -------------------------------------------------------------------
    // Read path
    // -------------------------------------------------------------------

    @Override
    public List<NearbyDriver> findNearby(NearbyQuery q) {
        String geoKey = GEO_AVAILABLE_PREFIX + q.vehicleType().name();

        GeoSearchCommandArgs args = GeoSearchCommandArgs.newGeoSearchArgs()
                .includeCoordinates()
                .includeDistance()
                .sortAscending()
                .limit(q.limit());

        // (lng, lat) — Redis-Geo convention
        Point center = new Point(q.lng(), q.lat());

        GeoResults<GeoLocation<String>> results = redis.opsForGeo().search(
                geoKey,
                GeoReference.fromCoordinate(center),
                GeoShape.byRadius(new Distance(q.radiusMeters(), DistanceUnit.METERS)),
                args
        );

        if (results == null || results.getContent().isEmpty()) {
            return List.of();
        }

        List<NearbyDriver> out = new ArrayList<>(results.getContent().size());
        for (GeoResult<GeoLocation<String>> r : results) {
            GeoLocation<String> loc = r.getContent();
            Point p = loc.getPoint();
            if (p == null) continue;       // shouldn't happen with WITHCOORD but defensive

            try {
                out.add(new NearbyDriver(
                        UUID.fromString(loc.getName()),
                        p.getY(),                       // lat
                        p.getX(),                       // lng
                        r.getDistance().getValue(),     // metres (WITHDIST + Metrics.METERS)
                        q.vehicleType()
                ));
            } catch (IllegalArgumentException e) {
                // Geo key holds a non-UUID member — possible if a buggy producer
                // ran in the past. Skip + log; alarming a dispatch flow on this
                // would be wrong.
                log.warn("Skipping malformed driver id in geo set key={} member={}", geoKey, loc.getName());
            }
        }
        return out;
    }

    // -------------------------------------------------------------------
    // Eviction path (stale-driver sweeper)
    // -------------------------------------------------------------------

    @Override
    public int evictStale(Duration olderThan) {
        long threshold = Instant.now().minus(olderThan).toEpochMilli();

        // Candidate stale drivers: heartbeat score (capturedAt) at or below the cutoff.
        Set<String> candidates = redis.opsForZSet().rangeByScore(HEARTBEAT_KEY, 0, threshold);
        if (candidates == null || candidates.isEmpty()) {
            return 0;
        }

        String thresholdArg = Long.toString(threshold);
        int evicted = 0;
        for (String driverId : candidates) {
            // KEYS = all geo shards, then heartbeat, then this driver's meta key.
            List<String> keys = new ArrayList<>(allGeoKeys.size() + 2);
            keys.addAll(allGeoKeys);
            keys.add(HEARTBEAT_KEY);
            keys.add(META_PREFIX + driverId);

            // Compare-and-remove: the Lua re-checks the heartbeat score, so a driver
            // that refreshed between the scan above and this call is left in place.
            Long result = redis.execute(evictScript, keys, driverId, thresholdArg);
            if (result != null && result == 1L) {
                evicted++;
            }
        }

        if (evicted > 0) {
            log.info("Swept {} stale driver(s) from geo index (older than {})", evicted, olderThan);
        }
        return evicted;
    }

    // -------------------------------------------------------------------
    // Availability-sync path (driver.availability-changed events)
    // -------------------------------------------------------------------

    @Override
    public void remove(UUID driverId) {
        // KEYS = all geo shards, then heartbeat, then this driver's meta key —
        // the layout remove-driver.lua expects (KEYS[1..N-2]=shards,
        // KEYS[N-1]=heartbeat, KEYS[N]=meta).
        List<String> keys = new ArrayList<>(allGeoKeys.size() + 2);
        keys.addAll(allGeoKeys);
        keys.add(HEARTBEAT_KEY);
        keys.add(META_PREFIX + driverId);

        redis.execute(removeScript, keys, driverId.toString());
    }

    @Override
    public boolean markBusy(UUID driverId, VehicleType vehicleType) {
        String vt = vehicleType.name();
        List<String> keys = List.of(
                GEO_AVAILABLE_PREFIX + vt,
                GEO_BUSY_PREFIX + vt,
                META_PREFIX + driverId);

        Long result = redis.execute(markBusyScript, keys, driverId.toString());
        return result != null && result == 1L;
    }
}
