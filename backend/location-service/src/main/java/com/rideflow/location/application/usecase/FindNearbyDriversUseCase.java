package com.rideflow.location.application.usecase;

import com.rideflow.location.domain.model.NearbyDriver;
import com.rideflow.location.domain.model.NearbyQuery;
import com.rideflow.location.domain.repository.DriverLocationRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FindNearbyDriversUseCase {

    private final DriverLocationRepository repository;
    private final Timer                    queryTimer;

    public FindNearbyDriversUseCase(DriverLocationRepository repository, MeterRegistry registry) {
        this.repository = repository;
        this.queryTimer = Timer.builder("rideflow.location.nearby.query")
                .description("Latency of GEOSEARCH-backed nearby driver queries")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public List<NearbyDriver> handle(NearbyQuery query) {
        return queryTimer.record(() -> repository.findNearby(query));
    }
}
