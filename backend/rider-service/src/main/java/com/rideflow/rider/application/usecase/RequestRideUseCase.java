package com.rideflow.rider.application.usecase;

import com.rideflow.rider.application.port.out.RideEventPublisher;
import com.rideflow.rider.application.port.out.RideRepository;
import com.rideflow.rider.application.port.out.RiderRepository;
import com.rideflow.rider.domain.event.RideRequested;
import com.rideflow.rider.domain.exception.RiderNotFoundException;
import com.rideflow.rider.domain.model.GeoPoint;
import com.rideflow.rider.domain.model.Ride;
import com.rideflow.rider.domain.model.VehicleType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Creates a ride request and emits a {@code RideRequested} event.
 *
 * <p>The whole method is one transaction: the ride row and the outbox row
 * commit together (or not at all). {@link RideEventPublisher} writes to the
 * outbox — it does NOT touch Kafka here — so there is no dual-write between the
 * database and the broker. The relay drains the outbox to Kafka after commit.
 */
@Service
public class RequestRideUseCase {

    private final RiderRepository    riderRepository;
    private final RideRepository     rideRepository;
    private final RideEventPublisher eventPublisher;

    public RequestRideUseCase(RiderRepository riderRepository,
                              RideRepository rideRepository,
                              RideEventPublisher eventPublisher) {
        this.riderRepository = riderRepository;
        this.rideRepository  = rideRepository;
        this.eventPublisher  = eventPublisher;
    }

    @Transactional
    public Ride request(RequestCommand cmd) {
        if (!riderRepository.existsById(cmd.riderId())) {
            throw new RiderNotFoundException(cmd.riderId());
        }

        Ride ride = Ride.request(
                cmd.riderId(),
                new GeoPoint(cmd.pickupLat(), cmd.pickupLng()),
                new GeoPoint(cmd.dropoffLat(), cmd.dropoffLng()),
                cmd.vehicleType());

        Ride saved = rideRepository.save(ride);

        eventPublisher.publishRideRequested(new RideRequested(
                saved.id(),
                saved.riderId(),
                saved.pickup().lat(),
                saved.pickup().lng(),
                saved.dropoff().lat(),
                saved.dropoff().lng(),
                saved.vehicleType().name(),
                saved.requestedAt()));

        return saved;
    }

    public record RequestCommand(
            UUID        riderId,
            double      pickupLat,
            double      pickupLng,
            double      dropoffLat,
            double      dropoffLng,
            VehicleType vehicleType) {}
}
