package com.rideflow.matching.api.dto.response;

import com.rideflow.matching.domain.model.Ride;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model for "what happened to this ride's dispatch." Backed by the
 * persisted terminal outcome, not live dispatch state.
 */
public record DispatchStatusResponse(
        UUID    rideId,
        String  status,
        UUID    assignedDriverId,
        Double  matchScore,
        String  failureReason,
        Instant assignedAt,
        Instant failedAt) {

    public static DispatchStatusResponse from(Ride r) {
        return new DispatchStatusResponse(
                r.id(),
                r.status().name(),
                r.assignedDriverId(),
                r.assignmentScore(),
                r.failureReason(),
                r.assignedAt(),
                r.failedAt());
    }
}
