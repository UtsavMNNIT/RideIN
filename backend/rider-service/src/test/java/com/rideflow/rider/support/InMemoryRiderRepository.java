package com.rideflow.rider.support;

import com.rideflow.rider.application.port.out.RiderRepository;
import com.rideflow.rider.domain.model.Rider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Hand-rolled in-memory fake for {@link RiderRepository} (no Mockito). */
public final class InMemoryRiderRepository implements RiderRepository {

    private final Map<UUID, Rider> byId = new HashMap<>();

    @Override
    public boolean existsByEmail(String email) {
        return byId.values().stream().anyMatch(r -> r.email().equals(email));
    }

    @Override
    public boolean existsByPhone(String phone) {
        return byId.values().stream().anyMatch(r -> r.phone().equals(phone));
    }

    @Override
    public boolean existsById(UUID id) {
        return byId.containsKey(id);
    }

    @Override
    public Optional<Rider> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Rider> findByEmail(String email) {
        return byId.values().stream().filter(r -> r.email().equals(email)).findFirst();
    }

    @Override
    public Rider save(Rider rider) {
        byId.put(rider.id(), rider);
        return rider;
    }

    /** Test helper: seed a rider directly. */
    public Rider seed(Rider rider) {
        byId.put(rider.id(), rider);
        return rider;
    }
}
