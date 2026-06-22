package com.rideflow.rider.support;

import com.rideflow.rider.application.port.out.RideRepository;
import com.rideflow.rider.domain.model.Ride;
import com.rideflow.rider.domain.model.RideStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Hand-rolled in-memory fake for {@link RideRepository} (no Mockito). */
public final class InMemoryRideRepository implements RideRepository {

    private final Map<UUID, Ride> byId = new HashMap<>();
    public int saveCount = 0;

    @Override
    public Ride save(Ride ride) {
        saveCount++;
        byId.put(ride.id(), ride);
        return ride;
    }

    @Override
    public Optional<Ride> findById(UUID rideId) {
        return Optional.ofNullable(byId.get(rideId));
    }

    @Override
    public List<Ride> findByRider(UUID riderId, int page, int size) {
        List<Ride> all = new ArrayList<>(byId.values());
        all.removeIf(r -> !r.riderId().equals(riderId));
        all.sort(Comparator.comparing(Ride::requestedAt).reversed());
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return all.subList(from, to);
    }

    @Override
    public List<Ride> findCompletedForDriver(UUID driverId, Instant from, Instant to) {
        List<Ride> all = new ArrayList<>(byId.values());
        all.removeIf(r -> r.assignedDriverId() == null
                || !r.assignedDriverId().equals(driverId)
                || r.status() != RideStatus.COMPLETED
                || (from != null && r.requestedAt().isBefore(from))
                || (to != null && !r.requestedAt().isBefore(to)));
        all.sort(Comparator.comparing(Ride::requestedAt).reversed());
        return all;
    }

    public Ride get(UUID rideId) {
        return byId.get(rideId);
    }
}
