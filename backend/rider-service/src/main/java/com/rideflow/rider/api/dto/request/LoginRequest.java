package com.rideflow.rider.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login payload. Only presence is validated; credential correctness yields a
 * uniform 401.
 */
public record LoginRequest(

        @NotBlank @Email
        String email,

        @NotBlank
        String password
) {}
