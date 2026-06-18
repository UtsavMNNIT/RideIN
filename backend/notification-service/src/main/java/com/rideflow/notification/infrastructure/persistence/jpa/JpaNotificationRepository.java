package com.rideflow.notification.infrastructure.persistence.jpa;

import com.rideflow.notification.application.port.out.NotificationRepository;
import com.rideflow.notification.domain.model.Notification;
import com.rideflow.notification.infrastructure.persistence.jpa.entity.NotificationEntity;
import com.rideflow.notification.infrastructure.persistence.jpa.repository.NotificationJpaRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Adapter implementing {@link NotificationRepository} on top of JPA.
 * Domain ↔ entity mapping is inlined (the shapes are nearly 1:1; a separate
 * mapper class would add ceremony without value).
 */
@Repository
public class JpaNotificationRepository implements NotificationRepository {

    private final NotificationJpaRepository jpa;

    public JpaNotificationRepository(NotificationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public boolean save(Notification n) {
        try {
            jpa.save(new NotificationEntity(
                    n.id(), n.userId(), n.role(), n.type(),
                    n.rideId(), n.payload(), n.createdAt()
            ));
            return true;
        } catch (DataIntegrityViolationException dup) {
            // Same notification id arrived twice — accept silently.
            return false;
        }
    }

    @Override
    public List<Notification> findUnreadForUser(UUID userId, int limit) {
        return jpa.findUnreadForUser(userId, PageRequest.of(0, limit))
                  .stream()
                  .map(JpaNotificationRepository::toDomain)
                  .toList();
    }

    @Override
    public List<Notification> findSince(UUID userId, Instant since, int limit) {
        return jpa.findSince(userId, since, PageRequest.of(0, limit))
                  .stream()
                  .map(JpaNotificationRepository::toDomain)
                  .toList();
    }

    @Override
    @Transactional
    public void markDelivered(UUID notificationId, Instant at) {
        jpa.findById(notificationId).ifPresent(e -> e.markDelivered(at));
    }

    private static Notification toDomain(NotificationEntity e) {
        return new Notification(
                e.getId(), e.getUserId(), e.getRole(), e.getType(),
                e.getRideId(), e.getPayload(), e.getCreatedAt()
        );
    }
}
