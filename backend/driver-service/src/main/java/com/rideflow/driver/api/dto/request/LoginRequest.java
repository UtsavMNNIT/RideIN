package com.rideflow.driver.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login payload. Only presence is validated here — credential correctness is a
 * use-case concern and any mismatch yields a uniform 401.
 */
public record LoginRequest(

        @NotBlank @Email
        String email,

        @NotBlank
        String password
) {}
