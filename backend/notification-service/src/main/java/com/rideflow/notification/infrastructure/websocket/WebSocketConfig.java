package com.rideflow.notification.infrastructure.websocket;

import com.rideflow.notification.infrastructure.security.HandshakeAuthInterceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * WebSocket wiring.
 *
 * <p>Endpoint: {@code /ws/notifications}. The handler is registered with the
 * handshake auth interceptor — unauthenticated upgrade requests are rejected
 * with HTTP 401, never reaching {@code afterConnectionEstablished}.
 *
 * <p><b>Backpressure.</b> Per-session send-buffer limits are enforced inside
 * {@link LocalSessionRegistry#deliver(java.util.UUID, String)} via a
 * per-session lock that bounds time spent on a slow consumer; combined with
 * the embedded Tomcat WebSocket send-buffer limit (configured by Spring Boot
 * defaults) this prevents a single misbehaving client from stalling the
 * dispatcher thread. Production hardening can layer
 * {@code ConcurrentWebSocketSessionDecorator} on top — deferred to keep the
 * session-identity story simple (the decorator complicates the
 * close-callback path).
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler handler;
    private final HandshakeAuthInterceptor     authInterceptor;
    private final String                       endpoint;
    private final String[]                     allowedOrigins;

    public WebSocketConfig(
            NotificationWebSocketHandler handler,
            HandshakeAuthInterceptor authInterceptor,
            @Value("${rideflow.websocket.endpoint}") String endpoint,
            @Value("${rideflow.websocket.allowed-origins}") String allowedOrigins) {
        this.handler         = handler;
        this.authInterceptor = authInterceptor;
        this.endpoint        = endpoint;
        this.allowedOrigins  = allowedOrigins.split(",");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, endpoint)
                .addInterceptors(authInterceptor, new HttpSessionHandshakeInterceptor())
                .setAllowedOrigins(allowedOrigins);
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
