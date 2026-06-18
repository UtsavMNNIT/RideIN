package com.rideflow.location.api.rest.v1;

import com.rideflow.location.api.dto.response.NearbyDriverResponse;
import com.rideflow.location.application.usecase.FindNearbyDriversUseCase;
import com.rideflow.location.domain.model.NearbyQuery;
import com.rideflow.location.domain.model.VehicleType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal endpoint — consumed by matching-service (and developer testing).
 * Behind the gateway it will require a service-token (Phase 3); for now,
 * exposed unauthenticated for end-to-end demos.
 *
 * <p>Path lives under {@code /api/v1/location/drivers/nearby} (NOT
 * {@code /api/v1/drivers/nearby}) to avoid colliding with driver-service's
 * own {@code /api/v1/drivers/*} namespace at the gateway.
 */
@RestController
@RequestMapping(value = "/api/v1/location/drivers", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class NearbyDriverController {

    private final FindNearbyDriversUseCase useCase;
    private final int maxRadius;
    private final int maxLimit;

    public NearbyDriverController(
            FindNearbyDriversUseCase useCase,
            @Value("${rideflow.location.nearby.max-radius-meters:10000}") int maxRadius,
            @Value("${rideflow.location.nearby.max-limit:50}")            int maxLimit) {
        this.useCase   = useCase;
        this.maxRadius = maxRadius;
        this.maxLimit  = maxLimit;
    }

    @GetMapping("/nearby")
    public List<NearbyDriverResponse> nearby(
            @RequestParam @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")    Double lat,
            @RequestParam @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")  Double lng,
            @RequestParam(defaultValue = "3000") @Min(1)                       int    radiusMeters,
            @RequestParam @NotNull                                             VehicleType vehicleType,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50)                int    limit
    ) {
        int boundedRadius = Math.min(radiusMeters, maxRadius);
        int boundedLimit  = Math.min(limit,        maxLimit);

        NearbyQuery query = new NearbyQuery(lat, lng, boundedRadius, vehicleType, boundedLimit);
        return useCase.handle(query).stream()
                .map(NearbyDriverResponse::from)
                .toList();
    }
}
