package com.rideflow.rider.api.dto.response;

import com.rideflow.rider.domain.model.DriverEarnings;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
        Instant    to,
        List<RecentTripResponse> recentTrips
) {

    /** One completed ride in the earnings trip list. */
    public record RecentTripResponse(
            UUID       rideId,
            Instant    completedAt,
            String     vehicleType,
            int        distanceMeters,
            BigDecimal fareTotal,
            String     currency
    ) {
        static RecentTripResponse from(DriverEarnings.RecentTrip t) {
            return new RecentTripResponse(
                    t.rideId(), t.completedAt(), t.vehicleType(),
                    t.distanceMeters(), t.fareTotal(), t.currency());
        }
    }

    public static EarningsResponse from(DriverEarnings e) {
        return new EarningsResponse(
                e.driverId(), e.totalFare(), e.currency(), e.completedTrips(),
                e.totalDistanceMeters(), e.totalDurationSeconds(), e.from(), e.to(),
                e.recentTrips().stream().map(RecentTripResponse::from).toList());
    }
}
