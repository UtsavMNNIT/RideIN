package com.rideflow.driver.api.dto.response;

import com.rideflow.driver.application.usecase.AuthenticateDriverUseCase;

import java.time.Instant;

/**
 * Login result: the bearer token, its lifetime metadata, and the driver
 * profile (so the client need not make a follow-up fetch).
 */
public record LoginResponse(
        String         accessToken,
        String         tokenType,
        long           expiresInSeconds,
        Instant        expiresAt,
        DriverResponse driver
) {

    public static LoginResponse from(AuthenticateDriverUseCase.Result result) {
        var token = result.token();
        return new LoginResponse(
                token.token(),
                token.tokenType(),
                token.expiresInSeconds(),
                token.expiresAt(),
                DriverResponse.from(result.driver()));
    }
}
