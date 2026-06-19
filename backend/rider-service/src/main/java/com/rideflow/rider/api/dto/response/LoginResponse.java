package com.rideflow.rider.api.dto.response;

import com.rideflow.rider.application.usecase.AuthenticateRiderUseCase;

import java.time.Instant;

/**
 * Login result: bearer token, lifetime metadata, and the rider profile.
 */
public record LoginResponse(
        String        accessToken,
        String        tokenType,
        long          expiresInSeconds,
        Instant       expiresAt,
        RiderResponse rider
) {

    public static LoginResponse from(AuthenticateRiderUseCase.Result result) {
        var token = result.token();
        return new LoginResponse(
                token.token(),
                token.tokenType(),
                token.expiresInSeconds(),
                token.expiresAt(),
                RiderResponse.from(result.rider()));
    }
}
