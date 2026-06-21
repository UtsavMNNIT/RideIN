package com.rideflow.location.application.usecase;

import com.rideflow.location.domain.model.DriverAvailability;
import com.rideflow.location.domain.model.DriverLocation;
import com.rideflow.location.domain.model.VehicleType;
import com.rideflow.location.domain.repository.DriverLocationRepository;
import com.rideflow.location.infrastructure.messaging.kafka.dto.DriverAvailabilityChangedPayloadDto;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Keeps the Redis geo availability index consistent with driver presence,
 * driven by {@code driver.availability-changed} events.
 *
 * <p>The location stream ({@code driver.location-updated}) only ever carries
 * ONLINE / ON_TRIP — a driver going OFFLINE stops pinging, so without this
 * stream an offline driver would linger in the index until the stale-sweeper
 * eventually evicts it (and could be offered rides in the meantime). This
 * use-case closes that gap by reacting to the authoritative presence signal:
 *
 * <ul>
 *   <li><b>OFFLINE</b> → remove the driver from the index entirely.</li>
 *   <li><b>ONLINE</b>  → if a last-known location is present, upsert into the
 *       AVAILABLE shard for the driver's vehicle type; otherwise no-op (the next
 *       location ping will index it).</li>
 *   <li><b>ON_TRIP</b> → move the driver to the BUSY shard: upsert with the
 *       carried location if present, else shift shards in place by driverId.</li>
 * </ul>
 *
 * <p>Redis set/remove operations are idempotent, so at-least-once redelivery is
 * harmless — no processed-events store is needed (location-service is Redis-only).
 *
 * <p>Any failure throws {@link IllegalArgumentException}, which the consumer's
 * {@link org.springframework.kafka.listener.DefaultErrorHandler} treats as
 * non-retriable → straight to DLQ.
 */
@Service
public class SyncDriverAvailabilityUseCase {

    private static final String ONLINE  = "ONLINE";
    private static final String OFFLINE = "OFFLINE";
    private static final String ON_TRIP = "ON_TRIP";

    private final DriverLocationRepository repository;
    private final Counter                  offlineCounter;
    private final Counter                  onlineCounter;
    private final Counter                  onTripCounter;

    public SyncDriverAvailabilityUseCase(DriverLocationRepository repository, MeterRegistry registry) {
        this.repository     = repository;
        this.offlineCounter = Counter.builder("rideflow.availability.applied")
                .description("Driver availability-changed events applied to the geo index")
                .tag("transition", "OFFLINE")
                .register(registry);
        this.onlineCounter  = Counter.builder("rideflow.availability.applied")
                .tag("transition", "ONLINE")
                .register(registry);
        this.onTripCounter  = Counter.builder("rideflow.availability.applied")
                .tag("transition", "ON_TRIP")
                .register(registry);
    }

    public void handle(DriverAvailabilityChangedPayloadDto p) {
        if (p == null)                  throw new IllegalArgumentException("payload is null");
        if (p.driverId() == null)       throw new IllegalArgumentException("driverId missing");
        if (p.availability() == null)   throw new IllegalArgumentException("availability missing");
        if (p.vehicleType() == null)    throw new IllegalArgumentException("vehicleType missing");

        VehicleType vehicleType = VehicleType.fromWire(p.vehicleType());

        switch (p.availability()) {
            case OFFLINE -> {
                repository.remove(p.driverId());
                offlineCounter.increment();
            }
            case ONLINE -> {
                if (hasLocation(p)) {
                    repository.upsert(toLocation(p, vehicleType, DriverAvailability.ONLINE));
                }
                // No location yet: the driver will be indexed on its next ping.
                onlineCounter.increment();
            }
            case ON_TRIP -> {
                if (hasLocation(p)) {
                    repository.upsert(toLocation(p, vehicleType, DriverAvailability.ON_TRIP));
                } else {
                    repository.markBusy(p.driverId(), vehicleType);
                }
                onTripCounter.increment();
            }
            default -> throw new IllegalArgumentException("unknown availability: " + p.availability());
        }
    }

    private static boolean hasLocation(DriverAvailabilityChangedPayloadDto p) {
        return p.lat() != null && p.lng() != null;
    }

    private static DriverLocation toLocation(DriverAvailabilityChangedPayloadDto p,
                                             VehicleType vehicleType,
                                             DriverAvailability availability) {
        // capturedAt: use the event's occurrence time so the out-of-order Lua
        // guard treats it as a current reading. Fall back to now() if absent.
        Instant capturedAt = p.occurredAt() != null ? p.occurredAt() : Instant.now();
        return new DriverLocation(
                p.driverId(),
                p.lat(),
                p.lng(),
                null,           // heading not carried by availability events
                null,           // speed not carried by availability events
                vehicleType,
                availability,
                capturedAt
        );
    }
}
