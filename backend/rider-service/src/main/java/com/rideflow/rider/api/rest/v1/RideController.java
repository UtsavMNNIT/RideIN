package com.rideflow.rider.api.rest.v1;

import com.rideflow.rider.api.dto.request.RequestRideRequest;
import com.rideflow.rider.api.dto.response.RideResponse;
import com.rideflow.rider.application.usecase.GetRideHistoryUseCase;
import com.rideflow.rider.application.usecase.RequestRideUseCase;
import com.rideflow.rider.domain.model.Ride;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Ride API, scoped under a rider.
 *
 * <pre>
 *   POST /v1/riders/{riderId}/rides        request a ride  → publishes RideRequested (via outbox)
 *   GET  /v1/riders/{riderId}/rides        ride history (newest-first, paged)
 * </pre>
 *
 * <p>In production the gateway authorizes {@code riderId} against the JWT
 * subject; this service does not run its own servlet security filter (same
 * trust model as the other RideFlow services).
 */
@RestController
@RequestMapping("/v1/riders/{riderId}/rides")
@Validated
public class RideController {

    private final RequestRideUseCase    requestRideUseCase;
    private final GetRideHistoryUseCase historyUseCase;
    private final int                   maxPageSize;

    public RideController(RequestRideUseCase requestRideUseCase,
                          GetRideHistoryUseCase historyUseCase,
                          @Value("${rideflow.ride.history.max-limit:100}") int maxPageSize) {
        this.requestRideUseCase = requestRideUseCase;
        this.historyUseCase     = historyUseCase;
        this.maxPageSize        = maxPageSize;
    }

    @PostMapping
    public ResponseEntity<RideResponse> request(@PathVariable UUID riderId,
                                                @Valid @RequestBody RequestRideRequest req) {
        Ride ride = requestRideUseCase.request(new RequestRideUseCase.RequestCommand(
                riderId,
                req.pickupLat(), req.pickupLng(),
                req.dropoffLat(), req.dropoffLng(),
                req.vehicleType()));

        return ResponseEntity
                .created(URI.create("/v1/riders/" + riderId + "/rides/" + ride.id()))
                .body(RideResponse.from(ride));
    }

    @GetMapping
    public List<RideResponse> history(
            @PathVariable UUID riderId,
            @RequestParam(defaultValue = "0")  @Min(0)             int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100)   int size) {

        int boundedSize = Math.min(size, maxPageSize);
        return historyUseCase.history(riderId, page, boundedSize).stream()
                .map(RideResponse::from)
                .toList();
    }
}
