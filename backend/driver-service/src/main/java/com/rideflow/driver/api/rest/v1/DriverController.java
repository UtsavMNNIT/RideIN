package com.rideflow.driver.api.rest.v1;

import com.rideflow.driver.api.dto.request.LoginRequest;
import com.rideflow.driver.api.dto.request.RegisterDriverRequest;
import com.rideflow.driver.api.dto.request.UpdateLocationRequest;
import com.rideflow.driver.api.dto.response.DriverResponse;
import com.rideflow.driver.api.dto.response.LoginResponse;
import com.rideflow.driver.application.usecase.AuthenticateDriverUseCase;
import com.rideflow.driver.application.usecase.DriverAvailabilityUseCase;
import com.rideflow.driver.application.usecase.RegisterDriverUseCase;
import com.rideflow.driver.application.usecase.UpdateDriverLocationUseCase;
import com.rideflow.driver.domain.model.Driver;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * Driver lifecycle API.
 *
 * <pre>
 *   POST /v1/drivers                      register
 *   POST /v1/drivers/login                authenticate → access token
 *   POST /v1/drivers/{driverId}/online    go online
 *   POST /v1/drivers/{driverId}/offline   go offline
 *   PUT  /v1/drivers/{driverId}/location  push a location update
 * </pre>
 *
 * <p>The controller is a thin delivery layer: validate, map DTO → use-case
 * command, map domain → response DTO. All business rules live in the
 * application/domain layers; all error mapping lives in
 * {@code GlobalExceptionHandler}.
 *
 * <p>In production the API gateway validates the JWT and injects the caller's
 * identity; {@code driverId} in the path is then authorized against the token
 * subject. This service does not run its own servlet security filter (same
 * trust model as the other RideFlow services).
 */
@RestController
@RequestMapping("/v1/drivers")
public class DriverController {

    private final RegisterDriverUseCase       registerUseCase;
    private final AuthenticateDriverUseCase   authenticateUseCase;
    private final DriverAvailabilityUseCase   availabilityUseCase;
    private final UpdateDriverLocationUseCase updateLocationUseCase;

    public DriverController(RegisterDriverUseCase registerUseCase,
                            AuthenticateDriverUseCase authenticateUseCase,
                            DriverAvailabilityUseCase availabilityUseCase,
                            UpdateDriverLocationUseCase updateLocationUseCase) {
        this.registerUseCase       = registerUseCase;
        this.authenticateUseCase   = authenticateUseCase;
        this.availabilityUseCase   = availabilityUseCase;
        this.updateLocationUseCase = updateLocationUseCase;
    }

    @PostMapping
    public ResponseEntity<DriverResponse> register(@Valid @RequestBody RegisterDriverRequest req) {
        Driver driver = registerUseCase.register(new RegisterDriverUseCase.RegisterCommand(
                req.email(), req.phone(), req.fullName(), req.password(),
                req.vehicleType(), req.vehiclePlate()));

        return ResponseEntity
                .created(URI.create("/v1/drivers/" + driver.id()))
                .body(DriverResponse.from(driver));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthenticateDriverUseCase.Result result = authenticateUseCase.login(req.email(), req.password());
        return ResponseEntity.ok(LoginResponse.from(result));
    }

    @PostMapping("/{driverId}/online")
    public ResponseEntity<DriverResponse> goOnline(@PathVariable UUID driverId) {
        return ResponseEntity.ok(DriverResponse.from(availabilityUseCase.goOnline(driverId)));
    }

    @PostMapping("/{driverId}/offline")
    public ResponseEntity<DriverResponse> goOffline(@PathVariable UUID driverId) {
        return ResponseEntity.ok(DriverResponse.from(availabilityUseCase.goOffline(driverId)));
    }

    @PutMapping("/{driverId}/location")
    public ResponseEntity<DriverResponse> updateLocation(@PathVariable UUID driverId,
                                                         @Valid @RequestBody UpdateLocationRequest req) {
        Instant capturedAt = req.capturedAt() != null ? req.capturedAt() : Instant.now();

        Driver driver = updateLocationUseCase.updateLocation(new UpdateDriverLocationUseCase.Command(
                driverId, req.lat(), req.lng(), req.headingDegrees(), req.speedMps(), capturedAt));

        return ResponseEntity.ok(DriverResponse.from(driver));
    }
}
