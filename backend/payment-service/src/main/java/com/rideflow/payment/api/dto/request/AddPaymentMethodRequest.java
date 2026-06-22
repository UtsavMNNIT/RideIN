package com.rideflow.payment.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Add a mock saved card. No real card data is accepted — just display metadata
 * (brand + last 4). The server mints an opaque token.
 */
public record AddPaymentMethodRequest(

        @NotNull
        UUID userId,

        @NotNull @Size(max = 32)
        String brand,

        @NotNull @Pattern(regexp = "^[0-9]{4}$", message = "last4 must be 4 digits")
        String last4,

        boolean isDefault
) {}
