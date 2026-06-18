package com.rideflow.notification.domain.model;

/**
 * The set of notification types this service emits.
 *
 * <p>Wire-stable: the name appears in persisted rows and on the WebSocket
 * frame's {@code type} field. Adding a value is safe; renaming is not.
 */
public enum NotificationType {
    /** A driver has been assigned to the rider's request. */
    RIDE_MATCHED,
    /** The driver has started the trip (pickup complete). */
    RIDE_STARTED,
    /** The trip has been completed. */
    RIDE_COMPLETED
}
