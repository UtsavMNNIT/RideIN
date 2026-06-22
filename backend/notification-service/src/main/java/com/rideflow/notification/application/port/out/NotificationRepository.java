package com.rideflow.notification.application.port.out;

import com.rideflow.notification.domain.model.Notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Output port for persisting notifications and reading them back for backfill. */
public interface NotificationRepository {

    boolean save(Notification notification);

    List<Notification> findUnreadForUser(UUID userId, int limit);

    List<Notification> findSince(UUID userId, Instant since, int limit);

    void markDelivered(UUID notificationId, Instant at);
}
