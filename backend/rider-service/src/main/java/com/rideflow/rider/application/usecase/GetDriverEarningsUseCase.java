package com.rideflow.rider.application.usecase;

import com.rideflow.rider.application.port.out.RideRepository;
import com.rideflow.rider.domain.model.DriverEarnings;
import com.rideflow.rider.domain.model.Ride;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Computes a driver's earnings from the rider-service ride read-model. The ride
 * projection already carries the assigned driver, the fare (from
 * {@code pricing.fare-quoted}) and the completion state (from
 * {@code ride.completed}), so earnings are a straight aggregation over the
 * driver's COMPLETED rides — no cross-service call needed.
 */
@Service
public class GetDriverEarningsUseCase {

    private static final String DEFAULT_CURRENCY = "INR";

    private final RideRepository rideRepository;

    public GetDriverEarningsUseCase(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    @Transactional(readOnly = true)
    public DriverEarnings earningsFor(UUID driverId, Instant from, Instant to) {
        List<Ride> rides = rideRepository.findCompletedForDriver(driverId, from, to);

        BigDecimal totalFare = BigDecimal.ZERO;
        long totalDistance   = 0L;
        long totalDuration   = 0L;
        String currency      = DEFAULT_CURRENCY;

        for (Ride r : rides) {
            if (r.fareTotal() != null) {
                totalFare = totalFare.add(r.fareTotal());
            }
            if (r.currency() != null) {
                currency = r.currency();
            }
            if (r.finalDistanceMeters() != null) {
                totalDistance += r.finalDistanceMeters();
            }
            if (r.finalDurationSeconds() != null) {
                totalDuration += r.finalDurationSeconds();
            }
        }

        return new DriverEarnings(
                driverId, totalFare, currency, rides.size(),
                totalDistance, totalDuration, from, to);
    }
}
