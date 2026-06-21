package com.rideflow.notification.support;

import com.rideflow.notification.application.port.out.NotificationRepository;
import com.rideflow.notification.domain.model.Notification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Hand-rolled in-memory fake (no Mockito) recording the args each read got. */
public final class InMemoryNotificationRepository implements NotificationRepository {

    public final List<Notification> unreadResult = new ArrayList<>();
    public final List<Notification> sinceResult = new ArrayList<>();

    public int unreadCalls = 0;
    public int sinceCalls = 0;
    public Integer lastUnreadLimit;
    public Integer lastSinceLimit;
    public Instant lastSince;
    public UUID lastUserId;

    @Override
    public boolean save(Notification notification) {
        return true;
    }

    @Override
    public List<Notification> findUnreadForUser(UUID userId, int limit) {
        unreadCalls++;
        lastUserId = userId;
        lastUnreadLimit = limit;
        return unreadResult;
    }

    @Override
    public List<Notification> findSince(UUID userId, Instant since, int limit) {
        sinceCalls++;
        lastUserId = userId;
        lastSince = since;
        lastSinceLimit = limit;
        return sinceResult;
    }

    @Override
    public void markDelivered(UUID notificationId, Instant at) {
        // no-op
    }
}
