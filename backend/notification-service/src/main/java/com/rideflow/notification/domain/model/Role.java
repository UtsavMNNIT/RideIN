package com.rideflow.notification.domain.model;

/**
 * The role a notification recipient holds in the ride. Drives subject-line
 * wording and routing (rider apps vs. driver apps).
 */
public enum Role {
    RIDER,
    DRIVER
}
