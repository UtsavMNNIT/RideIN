package com.rideflow.rider.api.rest.v1;

import com.rideflow.rider.api.dto.request.LoginRequest;
import com.rideflow.rider.api.dto.request.RegisterRiderRequest;
import com.rideflow.rider.api.dto.response.LoginResponse;
import com.rideflow.rider.api.dto.response.RiderResponse;
import com.rideflow.rider.application.usecase.AuthenticateRiderUseCase;
import com.rideflow.rider.application.usecase.RegisterRiderUseCase;
import com.rideflow.rider.domain.model.Rider;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Rider identity API.
 *
 * <pre>
 *   POST /v1/riders         register
 *   POST /v1/riders/login   authenticate → access token
 * </pre>
 */
@RestController
@RequestMapping("/v1/riders")
public class RiderController {

    private final RegisterRiderUseCase     registerUseCase;
    private final AuthenticateRiderUseCase authenticateUseCase;

    public RiderController(RegisterRiderUseCase registerUseCase,
                           AuthenticateRiderUseCase authenticateUseCase) {
        this.registerUseCase     = registerUseCase;
        this.authenticateUseCase = authenticateUseCase;
    }

    @PostMapping
    public ResponseEntity<RiderResponse> register(@Valid @RequestBody RegisterRiderRequest req) {
        Rider rider = registerUseCase.register(new RegisterRiderUseCase.RegisterCommand(
                req.email(), req.phone(), req.fullName(), req.password()));

        return ResponseEntity
                .created(URI.create("/v1/riders/" + rider.id()))
                .body(RiderResponse.from(rider));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthenticateRiderUseCase.Result result = authenticateUseCase.login(req.email(), req.password());
        return ResponseEntity.ok(LoginResponse.from(result));
    }
}
