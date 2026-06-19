package com.rideflow.notification.infrastructure.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.UUID;

import static com.rideflow.notification.infrastructure.websocket.HandshakeAttributes.ATTR_USER_ID;

/**
 * Bridges the framework's WebSocket lifecycle to the
 * {@link LocalSessionRegistry}. Incoming text frames are reserved for
 * client-side liveness pings (we echo a pong-equivalent ack) — application
 * messages flow server → client only.
 *
 * <p>Server-initiated {@link PingMessage} pings are sent on a separate
 * scheduled task (see {@code WebSocketHeartbeat}) so dead TCP is detected
 * within one heartbeat interval.
 */
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
    private static final TextMessage PING_ACK = new TextMessage("{\"type\":\"PONG\"}");

    private final LocalSessionRegistry registry;

    public NotificationWebSocketHandler(LocalSessionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID userId = (UUID) session.getAttributes().get(ATTR_USER_ID);
        if (userId == null) {
            // Handshake interceptor should have rejected the upgrade.
            log.warn("Session opened without userId attribute — closing sessionId={}", session.getId());
            close(session, CloseStatus.POLICY_VIOLATION);
            return;
        }
        registry.register(userId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID userId = (UUID) session.getAttributes().get(ATTR_USER_ID);
        if (userId != null) {
            registry.unregister(userId, session);
        }
        log.debug("Session closed sessionId={} status={}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Tiny client-protocol surface: clients may send {"type":"PING"} to
        // keep NAT mappings warm. We ack with a {"type":"PONG"}. Everything
        // else is ignored — this is a server → client channel.
        String body = message.getPayload();
        if (body != null && body.contains("\"PING\"")) {
            try {
                session.sendMessage(PING_ACK);
            } catch (IOException e) {
                log.debug("Failed to ack PING sessionId={}", session.getId(), e);
            }
        }
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) {
        // Receiving a pong implicitly proves liveness; no-op.
    }

    private void close(WebSocketSession s, CloseStatus reason) {
        try { s.close(reason); } catch (IOException ignored) { }
    }
}
