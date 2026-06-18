package com.rideflow.driver.api.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error envelope returned by {@code GlobalExceptionHandler}.
 * {@code fieldErrors} is populated only for validation failures.
 */
public record ApiError(
        Instant              timestamp,
        int                  status,
        String               error,
        String               message,
        String               path,
        List<FieldViolation> fieldErrors
) {

    public record FieldViolation(String field, String message) {}

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, List.of());
    }

    public static ApiError of(int status, String error, String message, String path,
                              List<FieldViolation> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
    }
}
