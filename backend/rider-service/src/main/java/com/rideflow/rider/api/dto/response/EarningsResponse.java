package com.rideflow.rider.api.dto.response;

import com.rideflow.rider.domain.model.DriverEarnings;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Public projection of a {@link DriverEarnings} aggregate. */
public record EarningsResponse(
        UUID       driverId,
        BigDecimal totalFare,
        String     currency,
        int        completedTrips,
        long       totalDistanceMeters,
        long       totalDurationSeconds,
        Instant    from,
        Instant    to
) {

    public static EarningsResponse from(DriverEarnings e) {
        return new EarningsResponse(
                e.driverId(), e.totalFare(), e.currency(), e.completedTrips(),
                e.totalDistanceMeters(), e.totalDurationSeconds(), e.from(), e.to());
    }
}
