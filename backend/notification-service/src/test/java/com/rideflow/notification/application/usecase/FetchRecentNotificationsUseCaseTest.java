package com.rideflow.notification.application.usecase;

import com.rideflow.notification.domain.model.Notification;
import com.rideflow.notification.domain.model.NotificationType;
import com.rideflow.notification.domain.model.Role;
import com.rideflow.notification.support.InMemoryNotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FetchRecentNotificationsUseCaseTest {

    private InMemoryNotificationRepository repository;
    private FetchRecentNotificationsUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryNotificationRepository();
        useCase = new FetchRecentNotificationsUseCase(repository);
    }

    @Test
    void sinceNull_usesUnreadLookup() {
        UUID user = UUID.randomUUID();
        repository.unreadResult.add(Notification.create(user, Role.RIDER,
                NotificationType.RIDE_MATCHED, UUID.randomUUID(), Map.of("k", "v")));

        List<Notification> result = useCase.fetchSince(user, null, 20);

        assertThat(repository.unreadCalls).isEqualTo(1);
        assertThat(repository.sinceCalls).isZero();
        assertThat(repository.lastUnreadLimit).isEqualTo(20);
        assertThat(repository.lastUserId).isEqualTo(user);
        assertThat(result).hasSize(1);
    }

    @Test
    void sinceSet_usesFindSince() {
        UUID user = UUID.randomUUID();
        Instant since = Instant.parse("2026-06-01T00:00:00Z");

        useCase.fetchSince(user, since, 20);

        assertThat(repository.sinceCalls).isEqualTo(1);
        assertThat(repository.unreadCalls).isZero();
        assertThat(repository.lastSince).isEqualTo(since);
        assertThat(repository.lastSinceLimit).isEqualTo(20);
    }

    @Test
    void limitClampedToMinimumOne() {
        useCase.fetchSince(UUID.randomUUID(), null, 0);
        assertThat(repository.lastUnreadLimit).isEqualTo(1);
    }

    @Test
    void negativeLimitClampedToOne() {
        useCase.fetchSince(UUID.randomUUID(), null, -50);
        assertThat(repository.lastUnreadLimit).isEqualTo(1);
    }

    @Test
    void limitClampedToMaximumHundred() {
        useCase.fetchSince(UUID.randomUUID(), Instant.now(), 5000);
        assertThat(repository.lastSinceLimit).isEqualTo(100);
    }

    @Test
    void limitWithinBoundsPassedThrough() {
        useCase.fetchSince(UUID.randomUUID(), null, 42);
        assertThat(repository.lastUnreadLimit).isEqualTo(42);
    }
}
