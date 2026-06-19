package com.rideflow.rider.api.dto.response;

import com.rideflow.rider.domain.model.Rider;

import java.time.Instant;
import java.util.UUID;

/**
 * Public projection of a {@link Rider}. Omits {@code passwordHash}.
 */
public record RiderResponse(
        UUID    id,
        String  email,
        String  phone,
        String  fullName,
        Instant createdAt
) {

    public static RiderResponse from(Rider r) {
        return new RiderResponse(r.id(), r.email(), r.phone(), r.fullName(), r.createdAt());
    }
}
