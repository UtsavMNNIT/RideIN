package com.rideflow.driver.support;

import com.rideflow.driver.application.port.out.DriverRepository;
import com.rideflow.driver.domain.model.Driver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Hand-rolled in-memory {@link DriverRepository} fake — no Mockito, no DB. */
public class InMemoryDriverRepository implements DriverRepository {

    private final Map<UUID, Driver> store = new HashMap<>();

    public InMemoryDriverRepository seed(Driver driver) {
        store.put(driver.id(), driver);
        return this;
    }

    @Override
    public boolean existsByEmail(String email) {
        return store.values().stream().anyMatch(d -> d.email().equals(email));
    }

    @Override
    public boolean existsByPhone(String phone) {
        return store.values().stream().anyMatch(d -> d.phone().equals(phone));
    }

    @Override
    public Optional<Driver> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Driver> findByEmail(String email) {
        return store.values().stream().filter(d -> d.email().equals(email)).findFirst();
    }

    @Override
    public Driver save(Driver driver) {
        store.put(driver.id(), driver);
        return driver;
    }
}
