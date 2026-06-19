package com.rideflow.gateway.infrastructure.filter;

import com.rideflow.gateway.infrastructure.config.GatewayProperties;
import com.rideflow.gateway.infrastructure.config.GatewayProperties.ForwardedHeaders;
import com.rideflow.gateway.infrastructure.security.AuthenticatedUser;
import com.rideflow.gateway.infrastructure.security.JwtValidationException;
import com.rideflow.gateway.infrastructure.security.JwtValidator;
import com.rideflow.gateway.infrastructure.security.PublicEndpointMatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * The platform's trust boundary. Every non-public request must carry a valid
 * {@code Authorization: Bearer <jwt>}; the gateway verifies it and forwards the
 * caller identity to downstream services as {@code X-User-*} headers.
 *
 * <h3>Anti-spoofing (critical)</h3>
 * Downstream services trust the {@code X-User-*} headers <em>because</em> they
 * can only come from the gateway. So this filter <b>unconditionally strips any
 * inbound {@code X-User-*} headers</b> — public route or not, authenticated or
 * not — before it injects its own. A client cannot forge an identity by setting
 * those headers itself.
 *
 * <h3>Ordering</h3>
 * Runs just after the access-logger and before the rate limiter, so the limiter
 * can key on the authenticated user it stashes in an exchange attribute.
 */
@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGlobalFilter.class);

    /** Exchange attribute holding the authenticated user id (read by the rate-limit key resolver). */
    public static final String USER_ID_ATTR = "rideflow.userId";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtValidator          validator;
    private final PublicEndpointMatcher publicEndpoints;
    private final ForwardedHeaders      headers;

    public JwtAuthenticationGlobalFilter(JwtValidator validator,
                                         PublicEndpointMatcher publicEndpoints,
                                         GatewayProperties props) {
        this.validator       = validator;
        this.publicEndpoints = publicEndpoints;
        this.headers         = props.forwardedHeaders();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. Always strip client-supplied identity headers — no spoofing, ever.
        ServerHttpRequest.Builder sanitized = request.mutate();
        headers.all().forEach(h -> sanitized.headers(hh -> hh.remove(h)));

        // 2. Public endpoints (login, register, health, CORS preflight) bypass auth.
        if (publicEndpoints.isPublic(request.getMethod(), request.getPath())) {
            return chain.filter(exchange.mutate().request(sanitized.build()).build());
        }

        // 3. Require a bearer token.
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, "missing or malformed Authorization header");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();

        // 4. Verify and inject the trusted identity headers.
        try {
            AuthenticatedUser user = validator.validate(token);

            sanitized.header(headers.userId(), user.userId());
            if (user.role() != null)  sanitized.header(headers.role(), user.role());
            if (user.email() != null) sanitized.header(headers.email(), user.email());

            exchange.getAttributes().put(USER_ID_ATTR, user.userId());

            return chain.filter(exchange.mutate().request(sanitized.build()).build());
        } catch (JwtValidationException e) {
            log.debug("Rejected request to {}: {}", request.getPath().value(), e.getMessage());
            return unauthorized(exchange, e.getMessage());
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        return GatewayErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED, message);
    }

    @Override
    public int getOrder() {
        // After the access logger (HIGHEST_PRECEDENCE), before the rate limiter (order 0).
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
