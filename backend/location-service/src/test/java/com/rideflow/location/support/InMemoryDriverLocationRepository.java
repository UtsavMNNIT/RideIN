package com.rideflow.location.support;

import com.rideflow.location.domain.model.DriverAvailability;
import com.rideflow.location.domain.model.DriverLocation;
import com.rideflow.location.domain.model.NearbyDriver;
import com.rideflow.location.domain.model.NearbyQuery;
import com.rideflow.location.domain.model.VehicleType;
import com.rideflow.location.domain.repository.DriverLocationRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hand-rolled in-memory fake (no Mockito). Models enough of the contract to
 * exercise the use cases: stale-guard upsert, nearest-first findNearby with
 * radius + vehicleType filter + limit, remove, markBusy, and a simple
 * heartbeat-based evictStale.
 */
public class InMemoryDriverLocationRepository implements DriverLocationRepository {

    private final Map<UUID, DriverLocation> store = new HashMap<>();
    /** Pre-seeded candidate set returned verbatim (post-filter) by findNearby. */
    private final List<NearbyDriver> nearbyCandidates = new ArrayList<>();

    public int upsertCount = 0;
    public int removeCount = 0;
    public int markBusyCount = 0;
    public final List<UUID> removed = new ArrayList<>();
    public final List<UUID> markedBusy = new ArrayList<>();

    @Override
    public boolean upsert(DriverLocation location) {
        upsertCount++;
        DriverLocation existing = store.get(location.driverId());
        // Stale guard: replays with same or older capturedAt are no-ops.
        if (existing != null && !location.capturedAt().isAfter(existing.capturedAt())) {
            return false;
        }
        store.put(location.driverId(), location);
        return true;
    }

    @Override
    public List<NearbyDriver> findNearby(NearbyQuery query) {
        List<NearbyDriver> matches = new ArrayList<>();
        for (NearbyDriver c : nearbyCandidates) {
            if (c.vehicleType() == query.vehicleType()
                    && c.distanceMeters() <= query.radiusMeters()) {
                matches.add(c);
            }
        }
        matches.sort(Comparator.comparingDouble(NearbyDriver::distanceMeters));
        if (matches.size() > query.limit()) {
            return new ArrayList<>(matches.subList(0, query.limit()));
        }
        return matches;
    }

    @Override
    public int evictStale(Duration olderThan) {
        // Caller-driven via evictStaleResult for deterministic assertions.
        return evictStaleResult;
    }

    public int evictStaleResult = 0;

    @Override
    public void remove(UUID driverId) {
        removeCount++;
        removed.add(driverId);
        store.remove(driverId);
    }

    @Override
    public boolean markBusy(UUID driverId, VehicleType vehicleType) {
        markBusyCount++;
        markedBusy.add(driverId);
        return true;
    }

    // ---- test helpers ----

    public void seedCandidate(NearbyDriver d) {
        nearbyCandidates.add(d);
    }

    public DriverLocation stored(UUID driverId) {
        return store.get(driverId);
    }

    public DriverAvailability availabilityOf(UUID driverId) {
        DriverLocation l = store.get(driverId);
        return l == null ? null : l.availability();
    }
}
