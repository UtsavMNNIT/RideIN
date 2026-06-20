package com.rideflow.notification.infrastructure.messaging.kafka.consumer;

import com.rideflow.notification.domain.model.Notification;
import com.rideflow.notification.domain.model.NotificationType;
import com.rideflow.notification.domain.model.Role;
import com.rideflow.notification.infrastructure.messaging.kafka.serde.DispatchFailedPayloadDto;
import com.rideflow.notification.infrastructure.messaging.kafka.serde.RideAssignedPayloadDto;
import com.rideflow.notification.infrastructure.messaging.kafka.serde.RideLifecyclePayloadDto;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link Notification} instances from typed payload DTOs.
 *
 * <p>Each source event fans into one or two notifications (rider and/or
 * driver). Payload shapes are deliberately small and audience-specific —
 * the rider sees driver info, the driver sees pickup info, neither sees raw
 * coordinates they don't need.
 */
@Component
public class NotificationFactory {

    public List<Notification> fromRideAssigned(RideAssignedPayloadDto p) {
        return List.of(
                Notification.create(
                        p.riderId(),
                        Role.RIDER,
                        NotificationType.RIDE_MATCHED,
                        p.rideId(),
                        riderRideMatchedPayload(p)
                ),
                Notification.create(
                        p.driverId(),
                        Role.DRIVER,
                        NotificationType.RIDE_MATCHED,
                        p.rideId(),
                        driverRideMatchedPayload(p)
                )
        );
    }

    public List<Notification> fromRideStarted(RideLifecyclePayloadDto p) {
        // Only the rider needs the "driver started the trip" cue; the driver
        // triggered it. We could surface a confirmation banner on the driver
        // side but that's a frontend concern.
        return List.of(
                Notification.create(
                        p.riderId(),
                        Role.RIDER,
                        NotificationType.RIDE_STARTED,
                        p.rideId(),
                        Map.of("rideId", p.rideId().toString(),
                               "occurredAt", String.valueOf(p.occurredAt()))
                )
        );
    }

    public List<Notification> fromRideCompleted(RideLifecyclePayloadDto p) {
        Map<String, Object> base = new LinkedHashMap<>();
        base.put("rideId",          p.rideId().toString());
        base.put("occurredAt",      String.valueOf(p.occurredAt()));
        if (p.distanceMeters()  != null) base.put("distanceMeters",  p.distanceMeters());
        if (p.durationSeconds() != null) base.put("durationSeconds", p.durationSeconds());
        if (p.fareTotal()       != null) base.put("fareTotal",       p.fareTotal().toPlainString());
        if (p.currency()        != null) base.put("currency",        p.currency());

        return List.of(
                Notification.create(p.riderId(),  Role.RIDER,  NotificationType.RIDE_COMPLETED, p.rideId(), base),
                Notification.create(p.driverId(), Role.DRIVER, NotificationType.RIDE_COMPLETED, p.rideId(), base)
        );
    }

    public List<Notification> fromDispatchFailed(DispatchFailedPayloadDto p) {
        // Only the rider is waiting on an answer; there is no driver to notify.
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rideId",      p.rideId().toString());
        m.put("vehicleType", p.vehicleType());
        if (p.reason()           != null) m.put("reason",           p.reason());
        if (p.attemptsMade()     != null) m.put("attemptsMade",     p.attemptsMade());
        if (p.lastRadiusMeters() != null) m.put("lastRadiusMeters", p.lastRadiusMeters());
        m.put("message", "No drivers available right now — please try again.");
        if (p.failedAt() != null) m.put("occurredAt", String.valueOf(p.failedAt()));

        return List.of(
                Notification.create(p.riderId(), Role.RIDER, NotificationType.NO_DRIVERS_FOUND, p.rideId(), m)
        );
    }

    public List<Notification> fromRideCancelled(RideLifecyclePayloadDto p) {
        // The rider is notified the trip was cancelled. cancelledBy/reason ride
        // on the wire but are not part of the lenient lifecycle DTO.
        return List.of(
                Notification.create(
                        p.riderId(),
                        Role.RIDER,
                        NotificationType.RIDE_CANCELLED,
                        p.rideId(),
                        Map.of("rideId", p.rideId().toString(),
                               "occurredAt", String.valueOf(p.occurredAt()))
                )
        );
    }

    private Map<String, Object> riderRideMatchedPayload(RideAssignedPayloadDto p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rideId",      p.rideId().toString());
        m.put("driverId",    p.driverId().toString());
        m.put("vehicleType", p.vehicleType());
        if (p.etaSeconds() != null) m.put("etaSeconds", p.etaSeconds());
        return m;
    }

    private Map<String, Object> driverRideMatchedPayload(RideAssignedPayloadDto p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rideId",     p.rideId().toString());
        m.put("riderId",    p.riderId().toString());
        m.put("pickupLat",  p.pickupLat());
        m.put("pickupLng",  p.pickupLng());
        m.put("dropoffLat", p.dropoffLat());
        m.put("dropoffLng", p.dropoffLng());
        return m;
    }
}
