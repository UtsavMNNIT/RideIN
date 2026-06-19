package com.rideflow.trip.api.dto.response;

import com.rideflow.trip.domain.model.Trip;

import java.time.Instant;
import java.util.UUID;

/** A trip as returned by the trip API. */
public record TripResponse(
        UUID    rideId,
        UUID    riderId,
        UUID    driverId,
        String  vehicleType,
        String  status,
        Double  matchScore,
        String  rejectReason,
        String  cancelledBy,
        String  cancelReason,
        Integer finalDistanceMeters,
        Integer finalDurationSeconds,
        Instant offeredAt,
        Instant offerExpiresAt,
        Instant acceptedAt,
        Instant arrivedAt,
        Instant startedAt,
        Instant completedAt,
        Instant rejectedAt,
        Instant cancelledAt) {

    public static TripResponse from(Trip t) {
        return new TripResponse(
                t.id(), t.riderId(), t.driverId(),
                t.vehicleType().name(), t.status().name(), t.matchScore(),
                t.rejectReason(),
                t.cancelledBy() == null ? null : t.cancelledBy().name(),
                t.cancelReason(),
                t.finalDistanceMeters(), t.finalDurationSeconds(),
                t.offeredAt(), t.offerExpiresAt(), t.acceptedAt(), t.arrivedAt(),
                t.startedAt(), t.completedAt(), t.rejectedAt(), t.cancelledAt());
    }
}
