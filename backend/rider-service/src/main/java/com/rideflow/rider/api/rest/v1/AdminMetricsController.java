package com.rideflow.rider.api.rest.v1;

import com.rideflow.rider.api.dto.response.OperatorMetricsResponse;
import com.rideflow.rider.application.usecase.GetOperatorMetricsUseCase;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operator (admin) metrics, served from the rider-service ride read-model.
 * Exposed under {@code /v1/admin/metrics}; the gateway routes {@code
 * /api/v1/admin/**} here and (for the demo) treats the metrics endpoint as
 * public since there is no ADMIN role yet.
 *
 * <pre>
 *   GET /v1/admin/metrics
 * </pre>
 */
@RestController
@RequestMapping("/v1/admin")
public class AdminMetricsController {

    private final GetOperatorMetricsUseCase metrics;

    public AdminMetricsController(GetOperatorMetricsUseCase metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/metrics")
    public OperatorMetricsResponse metrics() {
        return OperatorMetricsResponse.from(metrics.get());
    }
}
