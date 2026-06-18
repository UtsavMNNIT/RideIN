package com.rideflow.driver.api.rest.advice;

import com.rideflow.driver.api.dto.response.ApiError;
import com.rideflow.driver.domain.exception.DriverAlreadyExistsException;
import com.rideflow.driver.domain.exception.DriverNotFoundException;
import com.rideflow.driver.domain.exception.IllegalDriverStateException;
import com.rideflow.driver.domain.exception.InvalidCredentialsException;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Translates domain and framework exceptions into a uniform {@link ApiError}
 * with the correct HTTP status, keeping controllers free of try/catch and
 * status-mapping noise.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DriverAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleAlreadyExists(DriverAlreadyExistsException ex,
                                                        HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(DriverNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(DriverNotFoundException ex,
                                                   HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex,
                                                             HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalDriverStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalDriverStateException ex,
                                                       HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    /** Bean Validation failures on {@code @Valid @RequestBody}. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest req) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(),
                "Request validation failed", req.getRequestURI(), violations);
        return ResponseEntity.status(status).body(body);
    }

    /** Malformed JSON, wrong types, or an unparseable enum/UUID in the body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex,
                                                     HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body", req);
    }

    /** Defensive fallback — never leak internals to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", req);
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String message,
                                                  HttpServletRequest req) {
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(),
                message, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
