package com.rideflow.trip.support;

import com.rideflow.trip.application.port.out.TripRepository;
import com.rideflow.trip.domain.model.Trip;
import com.rideflow.trip.domain.model.TripStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Hand-rolled in-memory {@link TripRepository} fake — no Mockito, no DB. */
public class InMemoryTripRepository implements TripRepository {

    private final Map<UUID, Trip> store = new HashMap<>();

    public InMemoryTripRepository seed(Trip trip) {
        store.put(trip.id(), trip);
        return this;
    }

    @Override
    public Trip save(Trip trip) {
        store.put(trip.id(), trip);
        return trip;
    }

    @Override
    public Optional<Trip> findById(UUID rideId) {
        return Optional.ofNullable(store.get(rideId));
    }

    @Override
    public boolean existsById(UUID rideId) {
        return store.containsKey(rideId);
    }

    @Override
    public List<UUID> findExpiredOfferIds(Instant cutoff, int limit) {
        List<UUID> result = new ArrayList<>();
        for (Trip t : store.values()) {
            if (t.status() == TripStatus.OFFERED
                    && !t.offerExpiresAt().isAfter(cutoff)) {
                result.add(t.id());
                if (result.size() >= limit) break;
            }
        }
        return result;
    }
}
