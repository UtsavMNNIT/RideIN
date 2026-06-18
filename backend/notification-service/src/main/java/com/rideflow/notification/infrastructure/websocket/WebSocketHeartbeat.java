package com.rideflow.notification.infrastructure.websocket;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Scheduled task that pings every live session at the configured interval.
 *
 * <p><b>Why we ping from the server.</b> Idle TCP can survive a NAT timeout
 * or a client crash silently — the kernel keeps the socket "open" until a
 * write fails. Periodic application-level pings expose dead sessions within
 * one heartbeat interval, which is what the {@link LocalSessionRegistry}
 * relies on to keep its state honest.
 *
 * <p>The ping itself is a zero-payload {@link PingMessage}; healthy clients
 * respond with a pong that the framework consumes transparently.
 */
@Component
public class WebSocketHeartbeat {

    private final LocalSessionRegistry registry;

    public WebSocketHeartbeat(LocalSessionRegistry registry) {
        this.registry = registry;
    }

    @Scheduled(fixedDelayString = "${rideflow.websocket.heartbeat-interval-ms}")
    public void pingAll() {
        Map<UUID, Set<WebSocketSession>> all = registry.sessionsView();

        PingMessage ping = new PingMessage(ByteBuffer.allocate(0));
        all.forEach((userId, sessions) -> {
            WebSocketSession[] snap;
            synchronized (sessions) {
                snap = sessions.toArray(new WebSocketSession[0]);
            }
            for (WebSocketSession s : snap) {
                if (!s.isOpen()) {
                    registry.unregister(userId, s);
                    continue;
                }
                try {
                    s.sendMessage(ping);
                } catch (Exception e) {
                    registry.unregister(userId, s);
                    try { s.close(); } catch (Exception ignored) { }
                }
            }
        });
    }
}
