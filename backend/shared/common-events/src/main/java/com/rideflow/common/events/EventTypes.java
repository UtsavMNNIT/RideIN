package com.rideflow.common.events;

/**
 * Centralized literals for {@link EventEnvelope#eventType()}.
 *
 * Naming: {@code <context>.<PastTenseAction>} in PascalCase action.
 * Examples: "driver.LocationUpdated", "ride.Accepted".
 */
public final class EventTypes {

    private EventTypes() {}

    // ---- driver ----
    public static final String DRIVER_ONBOARDED            = "driver.Onboarded";
    public static final String DRIVER_AVAILABILITY_CHANGED = "driver.AvailabilityChanged";
    public static final String DRIVER_LOCATION_UPDATED     = "driver.LocationUpdated";

    // ---- rider / matching / pricing — declared up-front for cross-service grep ----
    public static final String RIDE_REQUESTED       = "rider.RideRequested";
    public static final String RIDE_ASSIGNED        = "matching.RideAssigned";
    public static final String RIDE_DISPATCH_FAILED = "matching.RideDispatchFailed";
    public static final String RIDE_ACCEPTED        = "ride.Accepted";
    public static final String RIDE_REJECTED        = "ride.Rejected";
    public static final String RIDE_STARTED         = "ride.Started";
    public static final String RIDE_COMPLETED       = "ride.Completed";
    public static final String RIDE_CANCELLED       = "ride.Cancelled";
    public static final String FARE_QUOTED          = "pricing.FareQuoted";

    // ---- payment ----
    public static final String PAYMENT_SETTLED      = "payment.Settled";
}
