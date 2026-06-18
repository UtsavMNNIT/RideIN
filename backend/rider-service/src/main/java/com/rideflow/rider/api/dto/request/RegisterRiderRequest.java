package com.rideflow.rider.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration payload. Password is bounded to 72 bytes (BCrypt's limit) so we
 * reject rather than silently truncate.
 */
public record RegisterRiderRequest(

        @NotBlank @Email @Size(max = 254)
        String email,

        @NotBlank
        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "must be a valid E.164 phone number")
        String phone,

        @NotBlank @Size(max = 120)
        String fullName,

        @NotBlank @Size(min = 8, max = 72)
        String password
) {}
