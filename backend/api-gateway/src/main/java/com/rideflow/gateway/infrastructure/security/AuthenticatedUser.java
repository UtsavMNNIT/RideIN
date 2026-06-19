package com.rideflow.gateway.infrastructure.security;

/**
 * The identity the gateway extracts from a verified JWT and forwards downstream.
 *
 * @param userId the token subject ({@code sub}) — the rider or driver id
 * @param role   {@code RIDER} or {@code DRIVER}
 * @param email  convenience claim (may be {@code null} for tokens that omit it)
 */
public record AuthenticatedUser(String userId, String role, String email) {
}
