package com.rideflow.driver.domain.model;

/**
 * Dispatch-facing presence state of a driver.
 *
 * <ul>
 *   <li>{@link #OFFLINE} — not accepting work; location pings are rejected.</li>
 *   <li>{@link #ONLINE}  — available for dispatch.</li>
 *   <li>{@link #ON_TRIP} — currently serving a ride. Owned by matching-service;
 *       reserved here so the state machine is closed.</li>
 * </ul>
 *
 * Kept in lock-step with the Postgres {@code driver.availability} enum.
 */
public enum DriverAvailability {
    OFFLINE,
    ONLINE,
    ON_TRIP
}
