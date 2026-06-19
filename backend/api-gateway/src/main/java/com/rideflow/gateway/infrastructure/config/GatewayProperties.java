package com.rideflow.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Bound from {@code rideflow.gateway.*} — the gateway's own policy knobs,
 * distinct from the Spring Cloud Gateway routing config under
 * {@code spring.cloud.gateway.*}.
 *
 * @param forwardedHeaders names of the identity headers injected downstream
 * @param publicEndpoints  method+path patterns that bypass JWT authentication
 */
@ConfigurationProperties(prefix = "rideflow.gateway")
public record GatewayProperties(
        ForwardedHeaders forwardedHeaders,
        List<PublicEndpoint> publicEndpoints
) {

    public GatewayProperties {
        forwardedHeaders = forwardedHeaders == null ? ForwardedHeaders.defaults() : forwardedHeaders;
        publicEndpoints  = publicEndpoints == null ? List.of() : List.copyOf(publicEndpoints);
    }

    /**
     * Header names carrying gateway-validated identity to downstream services.
     * Downstream trusts these <em>only because</em> the gateway strips any
     * client-supplied copies before setting them.
     */
    public record ForwardedHeaders(String userId, String role, String email) {
        public ForwardedHeaders {
            userId = blankToDefault(userId, "X-User-Id");
            role   = blankToDefault(role,   "X-User-Role");
            email  = blankToDefault(email,  "X-User-Email");
        }

        static ForwardedHeaders defaults() {
            return new ForwardedHeaders("X-User-Id", "X-User-Role", "X-User-Email");
        }

        /** All forwarded header names — the set stripped from inbound requests. */
        public List<String> all() {
            return List.of(userId, role, email);
        }

        private static String blankToDefault(String v, String def) {
            return (v == null || v.isBlank()) ? def : v;
        }
    }

    /**
     * An endpoint reachable without authentication (login, register, health, …).
     * {@code method} is a string (e.g. {@code POST}); {@code null}/blank matches
     * any method. Kept as a String to avoid relying on a String→HttpMethod
     * property-binding converter.
     */
    public record PublicEndpoint(String method, String pattern) {
        public PublicEndpoint {
            if (pattern == null || pattern.isBlank()) {
                throw new IllegalArgumentException("public-endpoint pattern is required");
            }
        }
    }
}
