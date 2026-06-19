package com.rideflow.matching.api.rest.v1;

import com.rideflow.matching.api.dto.response.DispatchStatusResponse;
import com.rideflow.matching.application.port.out.RideRepository;
import com.rideflow.matching.domain.exception.RideNotFoundException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read-only dispatch-status lookup. matching-service is event-driven; this
 * endpoint exists for operators and for the gateway to poll a ride's outcome.
 * There is intentionally no write API — dispatch is triggered only by the
 * {@code rider.ride-requested} stream.
 */
@RestController
@RequestMapping("/api/v1/dispatch")
public class DispatchQueryController {

    private final RideRepository rideRepository;

    public DispatchQueryController(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    @GetMapping("/rides/{rideId}")
    public DispatchStatusResponse getStatus(@PathVariable UUID rideId) {
        return rideRepository.findById(rideId)
                .map(DispatchStatusResponse::from)
                .orElseThrow(() -> new RideNotFoundException(rideId));
    }
}
