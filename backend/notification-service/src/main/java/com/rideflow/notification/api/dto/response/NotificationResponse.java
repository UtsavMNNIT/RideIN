package com.rideflow.notification.api.dto.response;

import com.rideflow.notification.domain.model.Notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP shape for a notification. Deliberately identical in structure to the
 * WebSocket frame payload so clients can use one parser for both channels.
 */
public record NotificationResponse(
        UUID                id,
        UUID                userId,
        String              role,
        String              type,
        UUID                rideId,
        Map<String, Object> payload,
        Instant             createdAt
) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.id(),
                n.userId(),
                n.role().name(),
                n.type().name(),
                n.rideId(),
                n.payload(),
                n.createdAt()
        );
    }
}
