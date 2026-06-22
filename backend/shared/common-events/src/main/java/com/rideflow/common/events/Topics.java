package com.rideflow.common.events;

/**
 * Centralized Kafka topic registry.
 *
 * Topics are provisioned by {@code infra/docker/kafka/topics-init.sh} —
 * keep this file and that script in lock-step.
 *
 * Naming convention: {@code <context>.<entity>-<past-tense-action>}.
 */
public final class Topics {

    private Topics() {}

    public static final String DRIVER_ONBOARDED            = "driver.onboarded";
    public static final String DRIVER_AVAILABILITY_CHANGED = "driver.availability-changed";
    public static final String DRIVER_LOCATION_UPDATED     = "driver.location-updated";

    public static final String RIDER_RIDE_REQUESTED        = "rider.ride-requested";
    public static final String MATCHING_RIDE_ASSIGNED      = "matching.ride-assigned";
    public static final String MATCHING_DISPATCH_FAILED    = "matching.ride-dispatch-failed";
    public static final String RIDE_ACCEPTED               = "ride.accepted";
    public static final String RIDE_REJECTED               = "ride.rejected";
    public static final String RIDE_STARTED                = "ride.started";
    public static final String RIDE_COMPLETED              = "ride.completed";
    public static final String RIDE_CANCELLED              = "ride.cancelled";
    public static final String PRICING_FARE_QUOTED         = "pricing.fare-quoted";
    public static final String PAYMENT_SETTLED             = "payment.settled";

    /** Suffix appended to a source topic to derive its DLQ. */
    public static final String DLQ_SUFFIX = ".DLQ";

    public static String dlqOf(String topic) {
        return topic + DLQ_SUFFIX;
    }
}
