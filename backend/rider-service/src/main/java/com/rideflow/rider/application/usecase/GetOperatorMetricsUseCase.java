package com.rideflow.rider.application.usecase;

import com.rideflow.rider.application.port.out.OperatorMetricsRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes the operator (admin) dashboard metrics from the ride read-model.
 * Read-only; the completion rate guards against division by zero.
 */
@Service
public class GetOperatorMetricsUseCase {

    private final OperatorMetricsRepository metrics;

    public GetOperatorMetricsUseCase(OperatorMetricsRepository metrics) {
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public OperatorMetrics get() {
        long completed    = metrics.completedRideCount();
        long unsuccessful = metrics.unsuccessfulRideCount();
        long settled      = completed + unsuccessful;
        double completionRate = settled == 0 ? 0.0 : (double) completed / settled;

        return new OperatorMetrics(
                metrics.activeRideCount(),
                metrics.onlineDriverCount(),
                metrics.totalRiders(),
                completionRate,
                Math.round(metrics.avgDispatchSeconds()));
    }

    /** Operator metrics snapshot. {@code avgDispatchSecs} is an approximation. */
    public record OperatorMetrics(
            long   activeRides,
            long   driversOnline,
            long   totalRiders,
            double completionRate,
            long   avgDispatchSecs) {
    }
}
