package com.rideflow.notification.infrastructure.security;

import com.rideflow.notification.domain.model.Role;
import com.rideflow.notification.infrastructure.websocket.HandshakeAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.UUID;

/**
 * Validates the WebSocket upgrade request and attaches identity attributes
 * to the resulting session.
 *
 * <h2>Portfolio-grade auth (dev mode)</h2>
 * Reads {@code ?userId=<uuid>&role=RIDER|DRIVER} from the upgrade URL. This is
 * deliberately permissive for local development and demo flows.
 *
 * <h2>Production wiring (next phase)</h2>
 * The {@code rideflow.auth.handshake.mode=jwt} branch will read a Bearer-style
 * token from a query parameter, validate via the shared
 * {@code common-security} JWT verifier, and extract user id + role from claims.
 * The interface presented to {@code WebSocketConfig} is unchanged.
 *
 * <p>Browsers cannot set custom headers on {@code new WebSocket(url)} — query
 * parameters are the only portable carrier for the token at handshake time.
 */
@Component
public class HandshakeAuthInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(HandshakeAuthInterceptor.class);

    private final String mode;
    private final String userIdParam;
    private final String roleParam;

    public HandshakeAuthInterceptor(
            @Value("${rideflow.auth.handshake.mode}") String mode,
            @Value("${rideflow.auth.handshake.user-id-param}") String userIdParam,
            @Value("${rideflow.auth.handshake.role-param}") String roleParam) {
        this.mode        = mode;
        this.userIdParam = userIdParam;
        this.roleParam   = roleParam;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletReq)
         || !(response instanceof ServletServerHttpResponse servletResp)) {
            return false;
        }
        HttpServletRequest http = servletReq.getServletRequest();

        if (!"query-param".equalsIgnoreCase(mode)) {
            // Future: 'jwt' mode plugs in here.
            reject(servletResp, "Unsupported auth mode: " + mode);
            return false;
        }

        String rawUserId = http.getParameter(userIdParam);
        String rawRole   = http.getParameter(roleParam);

        if (rawUserId == null || rawUserId.isBlank() || rawRole == null || rawRole.isBlank()) {
            reject(servletResp, "Missing " + userIdParam + " or " + roleParam);
            return false;
        }

        UUID userId;
        try {
            userId = UUID.fromString(rawUserId);
        } catch (IllegalArgumentException e) {
            reject(servletResp, "Invalid userId");
            return false;
        }

        Role role;
        try {
            role = Role.valueOf(rawRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            reject(servletResp, "Invalid role");
            return false;
        }

        attributes.put(HandshakeAttributes.ATTR_USER_ID, userId);
        attributes.put(HandshakeAttributes.ATTR_ROLE,    role);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.debug("Handshake failed", exception);
        }
    }

    private void reject(ServletServerHttpResponse resp, String reason) {
        log.debug("Handshake rejected: {}", reason);
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
    }
}
