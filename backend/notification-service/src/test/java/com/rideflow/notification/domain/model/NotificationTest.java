package com.rideflow.notification.domain.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    @Test
    void create_generatesIdAndTimestamp_andCopiesPayload() {
        UUID user = UUID.randomUUID();
        UUID ride = UUID.randomUUID();
        Map<String, Object> src = new HashMap<>();
        src.put("k", "v");

        Notification n = Notification.create(user, Role.RIDER, NotificationType.RIDE_STARTED, ride, src);

        assertThat(n.id()).isNotNull();
        assertThat(n.userId()).isEqualTo(user);
        assertThat(n.role()).isEqualTo(Role.RIDER);
        assertThat(n.type()).isEqualTo(NotificationType.RIDE_STARTED);
        assertThat(n.rideId()).isEqualTo(ride);
        assertThat(n.createdAt()).isNotNull();
        assertThat(n.payload()).containsEntry("k", "v");

        // Defensive copy: mutating the source map must not affect the notification.
        src.put("k2", "v2");
        assertThat(n.payload()).doesNotContainKey("k2");
    }

    @Test
    void create_payloadIsImmutable() {
        Notification n = Notification.create(UUID.randomUUID(), Role.DRIVER,
                NotificationType.RIDE_COMPLETED, UUID.randomUUID(), Map.of("a", 1));
        assertThatThrownBy(() -> n.payload().put("b", 2))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void constructor_rejectsNullRequiredFields() {
        assertThatThrownBy(() -> new Notification(null, UUID.randomUUID(), Role.RIDER,
                NotificationType.RIDE_MATCHED, UUID.randomUUID(), Map.of(), java.time.Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("id");
    }

    @Test
    void rideId_mayBeNull() {
        Notification n = new Notification(UUID.randomUUID(), UUID.randomUUID(), Role.RIDER,
                NotificationType.RIDE_MATCHED, null, Map.of(), java.time.Instant.now());
        assertThat(n.rideId()).isNull();
    }
}
