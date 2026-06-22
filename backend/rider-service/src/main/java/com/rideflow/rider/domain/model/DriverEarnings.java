package com.rideflow.rider.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregated earnings for a driver, derived from the rider-service ride
 * read-model (the service that projects the per-ride fare from
 * {@code pricing.fare-quoted} and the completion state from {@code ride.completed}).
 *
 * <p>Covers only {@link RideStatus#COMPLETED} rides assigned to the driver,
 * optionally bounded by a {@code [from, to)} request-time window.
 */
public record DriverEarnings(
        UUID       driverId,
        BigDecimal totalFare,
        String     currency,
        int        completedTrips,
        long       totalDistanceMeters,
        long       totalDurationSeconds,
        Instant    from,
        Instant    to
) {}
