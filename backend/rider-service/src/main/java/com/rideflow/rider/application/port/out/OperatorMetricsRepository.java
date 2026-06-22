package com.rideflow.rider.application.port.out;

/** Output port for operator (admin) metrics derived from the ride read-model. */
public interface OperatorMetricsRepository {

    long activeRideCount();

    /** Distinct drivers currently on an active ride (proxy for "online"). */
    long onlineDriverCount();

    long completedRideCount();

    /** Rides that ended without completing (cancelled + no-driver). */
    long unsuccessfulRideCount();

    long totalRiders();

    double avgDispatchSeconds();
}
