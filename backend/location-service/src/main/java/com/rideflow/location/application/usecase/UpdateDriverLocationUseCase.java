package com.rideflow.location.application.usecase;

import com.rideflow.location.domain.model.DriverAvailability;
import com.rideflow.location.domain.model.DriverLocation;
import com.rideflow.location.domain.model.VehicleType;
import com.rideflow.location.domain.repository.DriverLocationRepository;
import com.rideflow.location.infrastructure.messaging.kafka.dto.DriverLocationPayloadDto;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

/**
 * Triggered by the Kafka consumer; converts the wire DTO into a domain
 * {@link DriverLocation} (which re-validates invariants) and writes through
 * the repository.
 *
 * <p>This use-case is the only place we cross the wire/domain boundary,
 * so it owns:
 *   <ol>
 *     <li>Field-level null checks (missing JSON keys = bad event)</li>
 *     <li>Enum mapping (string → {@link VehicleType}/{@link DriverAvailability})</li>
 *     <li>Invariant enforcement via the domain record's compact ctor</li>
 *   </ol>
 * Any failure here throws {@link IllegalArgumentException}, which the
 * consumer's {@link org.springframework.kafka.listener.DefaultErrorHandler}
 * treats as non-retriable → straight to DLQ.
 */
@Service
public class UpdateDriverLocationUseCase {

    private final DriverLocationRepository repository;
    private final Counter                  appliedCounter;
    private final Counter                  staleCounter;

    public UpdateDriverLocationUseCase(DriverLocationRepository repository, MeterRegistry registry) {
        this.repository      = repository;
        this.appliedCounter  = Counter.builder("rideflow.location.applied")
                .description("Driver location upserts that hit Redis")
                .register(registry);
        this.staleCounter    = Counter.builder("rideflow.location.stale_dropped")
                .description("Out-of-order events dropped by the Lua guard")
                .register(registry);
    }

    public void handle(DriverLocationPayloadDto p) {
        if (p == null)                      throw new IllegalArgumentException("payload is null");
        if (p.driverId()   == null)         throw new IllegalArgumentException("driverId missing");
        if (p.lat()        == null)         throw new IllegalArgumentException("lat missing");
        if (p.lng()        == null)         throw new IllegalArgumentException("lng missing");
        if (p.vehicleType()  == null)       throw new IllegalArgumentException("vehicleType missing");
        if (p.availability() == null)       throw new IllegalArgumentException("availability missing");
        if (p.capturedAt()   == null)       throw new IllegalArgumentException("capturedAt missing");

        DriverLocation loc = new DriverLocation(
                p.driverId(),
                p.lat(),
                p.lng(),
                p.headingDegrees(),
                p.speedMps(),
                VehicleType.fromWire(p.vehicleType()),
                DriverAvailability.fromWire(p.availability()),
                p.capturedAt()
        );

        boolean applied = repository.upsert(loc);
        if (applied) {
            appliedCounter.increment();
        } else {
            staleCounter.increment();
        }
    }
}
