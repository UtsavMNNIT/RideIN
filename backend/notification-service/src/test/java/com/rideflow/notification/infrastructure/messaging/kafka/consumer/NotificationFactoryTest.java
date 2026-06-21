package com.rideflow.notification.infrastructure.messaging.kafka.consumer;

import com.rideflow.notification.domain.model.Notification;
import com.rideflow.notification.domain.model.NotificationType;
import com.rideflow.notification.domain.model.Role;
import com.rideflow.notification.infrastructure.messaging.kafka.serde.DispatchFailedPayloadDto;
import com.rideflow.notification.infrastructure.messaging.kafka.serde.RideAssignedPayloadDto;
import com.rideflow.notification.infrastructure.messaging.kafka.serde.RideLifecyclePayloadDto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationFactoryTest {

    private final NotificationFactory factory = new NotificationFactory();

    @Test
    void fromRideAssigned_fansToRiderAndDriver() {
        UUID rideId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        RideAssignedPayloadDto p = new RideAssignedPayloadDto(rideId, riderId, driverId,
                "STANDARD", 12.97, 77.59, 12.93, 77.62, 120, Instant.now());

        List<Notification> result = factory.fromRideAssigned(p);

        assertThat(result).hasSize(2);

        Notification rider = byRole(result, Role.RIDER);
        assertThat(rider.userId()).isEqualTo(riderId);
        assertThat(rider.type()).isEqualTo(NotificationType.RIDE_MATCHED);
        assertThat(rider.rideId()).isEqualTo(rideId);
        assertThat(rider.payload())
                .containsEntry("driverId", driverId.toString())
                .containsEntry("vehicleType", "STANDARD")
                .containsEntry("etaSeconds", 120);

        Notification driver = byRole(result, Role.DRIVER);
        assertThat(driver.userId()).isEqualTo(driverId);
        assertThat(driver.type()).isEqualTo(NotificationType.RIDE_MATCHED);
        assertThat(driver.payload())
                .containsEntry("riderId", riderId.toString())
                .containsEntry("pickupLat", 12.97)
                .containsEntry("dropoffLng", 77.62);
    }

    @Test
    void fromRideAssigned_omitsEtaWhenNull() {
        RideAssignedPayloadDto p = new RideAssignedPayloadDto(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "XL", 1.0, 2.0, 3.0, 4.0, null, Instant.now());
        Notification rider = byRole(factory.fromRideAssigned(p), Role.RIDER);
        assertThat(rider.payload()).doesNotContainKey("etaSeconds");
    }

    @Test
    void fromRideStarted_notifiesOnlyRider() {
        UUID rideId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-06-20T10:00:00Z");
        RideLifecyclePayloadDto p = new RideLifecyclePayloadDto(rideId, riderId,
                UUID.randomUUID(), occurredAt, null, null, null, null);

        List<Notification> result = factory.fromRideStarted(p);

        assertThat(result).hasSize(1);
        Notification rider = result.get(0);
        assertThat(rider.role()).isEqualTo(Role.RIDER);
        assertThat(rider.userId()).isEqualTo(riderId);
        assertThat(rider.type()).isEqualTo(NotificationType.RIDE_STARTED);
        assertThat(rider.payload())
                .containsEntry("rideId", rideId.toString())
                .containsEntry("occurredAt", occurredAt.toString());
    }

    @Test
    void fromRideCompleted_fansToRiderAndDriver_withMetrics() {
        UUID rideId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        RideLifecyclePayloadDto p = new RideLifecyclePayloadDto(rideId, riderId, driverId,
                Instant.now(), 4200, 900, new BigDecimal("253.50"), "INR");

        List<Notification> result = factory.fromRideCompleted(p);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(n ->
                assertThat(n.type()).isEqualTo(NotificationType.RIDE_COMPLETED));

        Notification rider = byRole(result, Role.RIDER);
        assertThat(rider.userId()).isEqualTo(riderId);
        assertThat(rider.payload())
                .containsEntry("distanceMeters", 4200)
                .containsEntry("durationSeconds", 900)
                .containsEntry("fareTotal", "253.50")
                .containsEntry("currency", "INR");

        Notification driver = byRole(result, Role.DRIVER);
        assertThat(driver.userId()).isEqualTo(driverId);
    }

    @Test
    void fromRideCompleted_omitsAbsentOptionalMetrics() {
        RideLifecyclePayloadDto p = new RideLifecyclePayloadDto(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), Instant.now(), null, null, null, null);
        Notification rider = byRole(factory.fromRideCompleted(p), Role.RIDER);
        assertThat(rider.payload())
                .doesNotContainKeys("distanceMeters", "durationSeconds", "fareTotal", "currency");
    }

    @Test
    void fromDispatchFailed_notifiesOnlyRider_withReasonAndMessage() {
        UUID rideId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        DispatchFailedPayloadDto p = new DispatchFailedPayloadDto(rideId, riderId,
                "STANDARD", "NO_SUPPLY", 3, 5000, Instant.parse("2026-06-20T10:00:00Z"));

        List<Notification> result = factory.fromDispatchFailed(p);

        assertThat(result).hasSize(1);
        Notification rider = result.get(0);
        assertThat(rider.role()).isEqualTo(Role.RIDER);
        assertThat(rider.userId()).isEqualTo(riderId);
        assertThat(rider.type()).isEqualTo(NotificationType.NO_DRIVERS_FOUND);
        assertThat(rider.payload())
                .containsEntry("vehicleType", "STANDARD")
                .containsEntry("reason", "NO_SUPPLY")
                .containsEntry("attemptsMade", 3)
                .containsEntry("lastRadiusMeters", 5000)
                .containsKey("message");
    }

    @Test
    void fromRideCancelled_notifiesOnlyRider() {
        UUID rideId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        RideLifecyclePayloadDto p = new RideLifecyclePayloadDto(rideId, riderId,
                UUID.randomUUID(), Instant.now(), null, null, null, null);

        List<Notification> result = factory.fromRideCancelled(p);

        assertThat(result).hasSize(1);
        Notification rider = result.get(0);
        assertThat(rider.role()).isEqualTo(Role.RIDER);
        assertThat(rider.userId()).isEqualTo(riderId);
        assertThat(rider.type()).isEqualTo(NotificationType.RIDE_CANCELLED);
        assertThat(rider.payload()).containsEntry("rideId", rideId.toString());
    }

    private static Notification byRole(List<Notification> list, Role role) {
        return list.stream().filter(n -> n.role() == role).findFirst().orElseThrow();
    }
}
