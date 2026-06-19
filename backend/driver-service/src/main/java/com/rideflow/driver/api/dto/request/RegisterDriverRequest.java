package com.rideflow.driver.api.dto.request;

import com.rideflow.driver.domain.model.VehicleType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration payload. Bean Validation rejects malformed input with a 400
 * before the use-case runs. Password is bounded to 72 bytes — BCrypt silently
 * truncates beyond that, so we reject rather than mislead the user.
 */
public record RegisterDriverRequest(

        @NotBlank @Email @Size(max = 254)
        String email,

        @NotBlank
        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "must be a valid E.164 phone number")
        String phone,

        @NotBlank @Size(max = 120)
        String fullName,

        @NotBlank @Size(min = 8, max = 72)
        String password,

        @NotNull
        VehicleType vehicleType,

        @NotBlank @Size(max = 16)
        String vehiclePlate
) {}
