package com.rideflow.trip.api.rest.v1;

import com.rideflow.trip.api.dto.request.CancelTripRequest;
import com.rideflow.trip.api.dto.request.CompleteTripRequest;
import com.rideflow.trip.api.dto.response.TripResponse;
import com.rideflow.trip.application.usecase.TripActionService;
import com.rideflow.trip.domain.model.TripMetrics;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Trip lifecycle API. Identity comes from the gateway-injected {@code X-User-Id}
 * / {@code X-User-Role} headers — never from the path alone. Driver actions are
 * authorized against the trip's driver; cancel against either bound party.
 *
 * <pre>
 *   POST /v1/trips/{rideId}/accept|reject|arrived|start   (driver)
 *   POST /v1/trips/{rideId}/complete                       (driver; body metrics)
 *   POST /v1/trips/{rideId}/cancel                         (rider or driver)
 *   GET  /v1/trips/{rideId}                                (rider or driver)
 * </pre>
 */
@RestController
@RequestMapping("/v1/trips")
public class TripController {

    private static final String HDR_USER = "X-User-Id";
    private static final String HDR_ROLE = "X-User-Role";

    private final TripActionService actions;

    public TripController(TripActionService actions) {
        this.actions = actions;
    }

    @PostMapping("/{rideId}/accept")
    public TripResponse accept(@PathVariable UUID rideId, @RequestHeader(HDR_USER) UUID userId) {
        return TripResponse.from(actions.accept(rideId, userId));
    }

    @PostMapping("/{rideId}/reject")
    public TripResponse reject(@PathVariable UUID rideId, @RequestHeader(HDR_USER) UUID userId) {
        return TripResponse.from(actions.reject(rideId, userId));
    }

    @PostMapping("/{rideId}/arrived")
    public TripResponse arrived(@PathVariable UUID rideId, @RequestHeader(HDR_USER) UUID userId) {
        return TripResponse.from(actions.arrive(rideId, userId));
    }

    @PostMapping("/{rideId}/start")
    public TripResponse start(@PathVariable UUID rideId, @RequestHeader(HDR_USER) UUID userId) {
        return TripResponse.from(actions.start(rideId, userId));
    }

    @PostMapping("/{rideId}/complete")
    public TripResponse complete(@PathVariable UUID rideId,
                                 @RequestHeader(HDR_USER) UUID userId,
                                 @Valid @RequestBody CompleteTripRequest req) {
        return TripResponse.from(actions.complete(rideId, userId,
                new TripMetrics(req.finalDistanceMeters(), req.finalDurationSeconds())));
    }

    @PostMapping("/{rideId}/cancel")
    public TripResponse cancel(@PathVariable UUID rideId,
                               @RequestHeader(HDR_USER) UUID userId,
                               @RequestHeader(HDR_ROLE) String role,
                               @Valid @RequestBody(required = false) CancelTripRequest req) {
        String reason = req == null ? null : req.reason();
        return TripResponse.from(actions.cancel(rideId, userId, role, reason));
    }

    @GetMapping("/{rideId}")
    public TripResponse get(@PathVariable UUID rideId, @RequestHeader(HDR_USER) UUID userId) {
        return TripResponse.from(actions.get(rideId, userId));
    }
}
