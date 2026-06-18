package com.rideflow.notification.application.usecase;

import com.rideflow.notification.application.port.out.NotificationRepository;
import com.rideflow.notification.domain.model.Notification;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Backfill use case: a client reconnects after missing real-time pushes and
 * wants everything since its last seen timestamp. Caps the page so a long-idle
 * client can't pull the entire history in one request.
 */
@Service
public class FetchRecentNotificationsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository repository;

    public FetchRecentNotificationsUseCase(NotificationRepository repository) {
        this.repository = repository;
    }

    public List<Notification> fetchSince(UUID userId, Instant since, int requestedLimit) {
        int limit = Math.min(Math.max(requestedLimit, 1), MAX_PAGE_SIZE);
        if (since == null) {
            return repository.findUnreadForUser(userId, limit);
        }
        return repository.findSince(userId, since, limit);
    }
}
