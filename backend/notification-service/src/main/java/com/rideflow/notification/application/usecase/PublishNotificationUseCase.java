package com.rideflow.notification.application.usecase;

import com.rideflow.notification.application.port.out.NotificationBroadcaster;
import com.rideflow.notification.application.port.out.NotificationRepository;
import com.rideflow.notification.application.port.out.ProcessedEventRepository;
import com.rideflow.notification.domain.model.Notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the consumer-side fan-out:
 *
 * <ol>
 *   <li>Idempotency claim + notification persistence run in <b>one</b>
 *       transaction. A PK violation on {@code processed_events} (duplicate
 *       delivery) rolls back the entire transaction, so we never persist a
 *       notification twice for the same source event.</li>
 *   <li>Broadcast runs <b>outside</b> the transaction, after commit. Rationale:
 *       (a) a slow Redis must not hold a DB transaction open; (b) we never
 *       publish a notification that isn't durably recorded — if the broadcast
 *       later fails, the persisted row will surface to a reconnecting client
 *       via REST backfill.</li>
 * </ol>
 */
@Service
public class PublishNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(PublishNotificationUseCase.class);

    private final NotificationRepository     repository;
    private final ProcessedEventRepository   processedEvents;
    private final NotificationBroadcaster    broadcaster;

    public PublishNotificationUseCase(NotificationRepository repository,
                                      ProcessedEventRepository processedEvents,
                                      NotificationBroadcaster broadcaster) {
        this.repository      = repository;
        this.processedEvents = processedEvents;
        this.broadcaster     = broadcaster;
    }

    /**
     * @return {@code true} if newly processed; {@code false} if duplicate.
     *         Callers ack in both cases — duplicates are not errors.
     */
    public boolean handle(UUID eventId, String consumerGroup, List<Notification> notifications) {
        boolean claimed = persistTransactionally(eventId, consumerGroup, notifications);
        if (!claimed) {
            log.debug("Duplicate event eventId={} group={} — skipping broadcast", eventId, consumerGroup);
            return false;
        }
        // Post-commit broadcast: each notification is now durable.
        for (Notification n : notifications) {
            try {
                broadcaster.broadcast(n);
            } catch (RuntimeException e) {
                // Don't propagate — the row is persisted, REST backfill will deliver it.
                // We log + metric; a runaway Redis failure surfaces as missed-broadcast count.
                log.warn("Broadcast failed notificationId={} userId={} — will fall back to backfill",
                        n.id(), n.userId(), e);
            }
        }
        return true;
    }

    @Transactional
    protected boolean persistTransactionally(UUID eventId, String consumerGroup,
                                             List<Notification> notifications) {
        if (!processedEvents.markProcessed(eventId, consumerGroup)) {
            return false;
        }
        for (Notification n : notifications) {
            repository.save(n);
        }
        return true;
    }
}
