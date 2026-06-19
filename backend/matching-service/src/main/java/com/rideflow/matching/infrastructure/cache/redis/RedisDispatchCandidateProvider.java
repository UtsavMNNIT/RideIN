package com.rideflow.matching.infrastructure.cache.redis;

import com.rideflow.matching.domain.model.DispatchCandidate;
import com.rideflow.matching.domain.model.GeoPoint;
import com.rideflow.matching.domain.model.VehicleType;
import com.rideflow.matching.domain.repository.DispatchCandidateProvider;
import com.rideflow.matching.infrastructure.config.DispatchProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.DistanceUnit;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.GeoShape;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the Redis-Geo index that location-service maintains and returns
 * dispatchable candidates around a pickup.
 *
 * <h2>Two-step read</h2>
 * <ol>
 *   <li><b>{@code GEOSEARCH}</b> on {@code geo:drivers:available:<vehicleType>}
 *       — one command, sub-millisecond, returns the K nearest members with
 *       coordinate + distance, sorted ascending. The shard name already scopes
 *       us to the right vehicle class and to drivers location-service believes
 *       are available.</li>
 *   <li><b>Pipelined {@code HMGET}</b> on each {@code driver:meta:<id>} hash —
 *       one round-trip for all K, pulling availability + heading + speed +
 *       last-heartbeat. This is where we <em>re-verify</em> availability and
 *       freshness rather than trusting the shard, and where heading is sourced
 *       for the scorer.</li>
 * </ol>
 *
 * <h2>Filtering (requirement: "filter unavailable drivers")</h2>
 * A candidate is dropped if its metadata hash is gone (TTL expired ⇒ the driver
 * went dark), if availability is no longer {@code ONLINE}, or if the last
 * heartbeat is older than the configured staleness window. A driver can linger
 * in the geo index for a few seconds after going dark; this guard ensures we
 * never offer a ride to one of them.
 */
@Repository
public class RedisDispatchCandidateProvider implements DispatchCandidateProvider {

    private static final Logger log = LoggerFactory.getLogger(RedisDispatchCandidateProvider.class);

    private static final String GEO_AVAILABLE_PREFIX = "geo:drivers:available:";
    private static final String META_PREFIX          = "driver:meta:";

    // Metadata hash field names (written by location-service's Lua upsert).
    private static final byte[] F_AVAILABILITY = "availability".getBytes(StandardCharsets.UTF_8);
    private static final byte[] F_HEADING      = "heading".getBytes(StandardCharsets.UTF_8);
    private static final byte[] F_SPEED        = "speed".getBytes(StandardCharsets.UTF_8);
    private static final byte[] F_CAPTURED_AT  = "capturedAt".getBytes(StandardCharsets.UTF_8);

    private static final String STATUS_ONLINE = "ONLINE";

    private final StringRedisTemplate redis;
    private final long                stalenessMs;

    public RedisDispatchCandidateProvider(
            @Qualifier("geoRedisTemplate") StringRedisTemplate redis,
            DispatchProperties props) {
        this.redis       = redis;
        this.stalenessMs = props.candidateStalenessMs();
    }

    @Override
    public List<DispatchCandidate> findCandidates(GeoPoint pickup, VehicleType vehicleType,
                                                  int radiusMeters, int limit) {
        String geoKey = GEO_AVAILABLE_PREFIX + vehicleType.name();

        GeoResults<GeoLocation<String>> geo = geoSearch(geoKey, pickup, radiusMeters, limit);
        if (geo == null || geo.getContent().isEmpty()) {
            return List.of();
        }

        // Preserve GEOSEARCH order so the pipeline results line up by index.
        List<GeoResult<GeoLocation<String>>> ordered = new ArrayList<>(geo.getContent());
        List<String> ids = ordered.stream().map(r -> r.getContent().getName()).toList();

        List<List<String>> metas = pipelineMeta(ids);

        long nowMs = Instant.now().toEpochMilli();
        List<DispatchCandidate> out = new ArrayList<>(ordered.size());

        for (int i = 0; i < ordered.size(); i++) {
            GeoResult<GeoLocation<String>> r = ordered.get(i);
            GeoLocation<String> loc = r.getContent();
            Point p = loc.getPoint();
            if (p == null) continue;                     // defensive; WITHCOORD should fill it

            List<String> meta = i < metas.size() ? metas.get(i) : null;
            if (!isDispatchable(loc.getName(), meta, nowMs)) {
                continue;
            }

            java.util.UUID driverId;
            try {
                driverId = java.util.UUID.fromString(loc.getName());
            } catch (IllegalArgumentException e) {
                log.warn("Skipping malformed driver id in geo key={} member={}", geoKey, loc.getName());
                continue;
            }

            out.add(new DispatchCandidate(
                    driverId,
                    new GeoPoint(p.getY(), p.getX()),            // (lat, lng)
                    r.getDistance().getValue(),                 // metres
                    parseDouble(meta.get(1)),                   // heading (nullable)
                    parseDouble(meta.get(2)),                   // speed   (nullable)
                    null));                                     // unscored
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Redis calls
    // ------------------------------------------------------------------

    private GeoResults<GeoLocation<String>> geoSearch(String geoKey, GeoPoint pickup,
                                                       int radiusMeters, int limit) {
        GeoSearchCommandArgs args = GeoSearchCommandArgs.newGeoSearchArgs()
                .includeCoordinates()
                .includeDistance()
                .sortAscending()
                .limit(limit);

        Point center = new Point(pickup.lng(), pickup.lat());   // (lng, lat) — Redis convention

        return redis.opsForGeo().search(
                geoKey,
                GeoReference.fromCoordinate(center),
                GeoShape.byRadius(new Distance(radiusMeters, DistanceUnit.METERS)),
                args);
    }

    /**
     * One round-trip HMGET for every candidate. Each result list is
     * {@code [availability, heading, speed, capturedAt]} with {@code null}
     * entries for missing fields (and all-null if the hash has expired).
     */
    @SuppressWarnings("unchecked")
    private List<List<String>> pipelineMeta(List<String> ids) {
        List<Object> raw = redis.executePipelined((RedisCallback<Object>) connection -> {
            for (String id : ids) {
                byte[] key = (META_PREFIX + id).getBytes(StandardCharsets.UTF_8);
                connection.hashCommands().hMGet(key, F_AVAILABILITY, F_HEADING, F_SPEED, F_CAPTURED_AT);
            }
            return null;
        }, RedisSerializer.string());

        List<List<String>> metas = new ArrayList<>(raw.size());
        for (Object o : raw) {
            metas.add(o instanceof List<?> list ? (List<String>) list : List.of());
        }
        return metas;
    }

    // ------------------------------------------------------------------
    // Filtering
    // ------------------------------------------------------------------

    private boolean isDispatchable(String driverId, List<String> meta, long nowMs) {
        if (meta == null || meta.size() < 4) {
            log.debug("Dropping driver {} — metadata missing/expired", driverId);
            return false;
        }
        String availability = meta.get(0);
        String capturedAt   = meta.get(3);

        if (!STATUS_ONLINE.equals(availability)) {
            log.debug("Dropping driver {} — availability={}", driverId, availability);
            return false;
        }
        Long captured = parseLong(capturedAt);
        if (captured == null || (nowMs - captured) > stalenessMs) {
            log.debug("Dropping driver {} — stale heartbeat (capturedAt={})", driverId, capturedAt);
            return false;
        }
        return true;
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.valueOf(s); } catch (NumberFormatException e) { return null; }
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.valueOf(s); } catch (NumberFormatException e) { return null; }
    }
}
