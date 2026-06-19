package com.rideflow.matching.api.dto.response;

import java.time.Instant;

/** Uniform error envelope returned by {@code GlobalExceptionHandler}. */
public record ApiError(
        Instant timestamp,
        int     status,
        String  error,
        String  message,
        String  path) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path);
    }
}
