package com.rideflow.rider.api.rest.v1;

import com.rideflow.rider.api.dto.response.EarningsResponse;
import com.rideflow.rider.application.usecase.GetDriverEarningsUseCase;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * Driver earnings, served from the rider-service ride read-model (which owns the
 * per-ride fare + completion state). Exposed under {@code /v1/earnings} so the
 * gateway can route it independently of the rider-scoped {@code /v1/riders/**}
 * tree; the gateway authorizes the caller's JWT.
 *
 * <pre>
 *   GET /v1/earnings/drivers/{driverId}?from={iso}&to={iso}
 * </pre>
 */
@RestController
@RequestMapping("/v1/earnings")
public class DriverEarningsController {

    private final GetDriverEarningsUseCase earningsUseCase;

    public DriverEarningsController(GetDriverEarningsUseCase earningsUseCase) {
        this.earningsUseCase = earningsUseCase;
    }

    @GetMapping("/drivers/{driverId}")
    public EarningsResponse earnings(
            @PathVariable UUID driverId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return EarningsResponse.from(earningsUseCase.earningsFor(driverId, from, to));
    }
}
