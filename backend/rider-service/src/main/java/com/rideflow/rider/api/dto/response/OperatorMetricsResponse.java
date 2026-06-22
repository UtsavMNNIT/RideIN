package com.rideflow.rider.api.dto.response;

import com.rideflow.rider.application.usecase.GetOperatorMetricsUseCase.OperatorMetrics;

/**
 * Operator dashboard metrics. Field names match the frontend
 * {@code OperatorMetrics} type exactly.
 */
public record OperatorMetricsResponse(
        long   activeRides,
        long   driversOnline,
        long   totalRiders,
        double completionRate,
        long   avgDispatchSecs) {

    public static OperatorMetricsResponse from(OperatorMetrics m) {
        return new OperatorMetricsResponse(
                m.activeRides(), m.driversOnline(), m.totalRiders(),
                m.completionRate(), m.avgDispatchSecs());
    }
}
