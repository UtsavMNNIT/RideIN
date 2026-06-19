package com.rideflow.driver.application.usecase;

import com.rideflow.driver.application.port.out.DriverRepository;
import com.rideflow.driver.domain.event.DomainEventPublisher;
import com.rideflow.driver.domain.event.DriverLocationUpdated;
import com.rideflow.driver.domain.exception.DriverNotFoundException;
import com.rideflow.driver.domain.model.Driver;
import com.rideflow.driver.domain.model.GeoPoint;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Records a driver's latest location and publishes it as a
 * {@link DriverLocationUpdated} event for downstream consumers (location- and
 * matching-service).
 *
 * <p><b>Ordering:</b> persistence first, publish after. The Kafka adapter is
 * non-blocking and location telemetry is lossy-acceptable, so the event is
 * emitted only once the last-known location is durably stored — we never
 * advertise a position we failed to persist. We intentionally do not wrap this
 * in a single transaction: {@code save} is its own short transaction and the
 * publish must observe the committed state.
 */
@Service
public class UpdateDriverLocationUseCase {

    private final DriverRepository     repository;
    private final DomainEventPublisher eventPublisher;

    public UpdateDriverLocationUseCase(DriverRepository repository,
                                       DomainEventPublisher eventPublisher) {
        this.repository     = repository;
        this.eventPublisher = eventPublisher;
    }

    public Driver updateLocation(Command cmd) {
        Driver driver = repository.findById(cmd.driverId())
                .orElseThrow(() -> new DriverNotFoundException(cmd.driverId()));

        // Domain enforces "must be ONLINE/ON_TRIP"; throws IllegalDriverStateException otherwise.
        Driver updated = driver.updateLocation(new GeoPoint(cmd.lat(), cmd.lng()), cmd.capturedAt());
        Driver saved   = repository.save(updated);

        eventPublisher.publishLocationUpdate(new DriverLocationUpdated(
                saved.id(),
                cmd.lat(),
                cmd.lng(),
                cmd.headingDegrees(),
                cmd.speedMps(),
                saved.vehicleType().name(),
                saved.availability().name(),
                cmd.capturedAt()));

        return saved;
    }

    /**
     * Use-case input. {@code headingDegrees} and {@code speedMps} are optional
     * telemetry; {@code capturedAt} is the device timestamp (defaulted to
     * {@code now()} by the controller when the client omits it).
     */
    public record Command(
            UUID    driverId,
            double  lat,
            double  lng,
            Double  headingDegrees,
            Double  speedMps,
            Instant capturedAt) {}
}
