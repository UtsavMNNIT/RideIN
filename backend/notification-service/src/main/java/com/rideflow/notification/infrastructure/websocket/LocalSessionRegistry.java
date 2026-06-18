package com.rideflow.notification.infrastructure.websocket;

import com.rideflow.notification.application.port.out.SessionRegistry;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-JVM registry of live WebSocket sessions, keyed by user id.
 *
 * <p>One user may hold multiple concurrent sessions (web tab + mobile app).
 * {@link #deliver(UUID, String)} fans out to every local session for the user;
 * the cluster-wide fan-out happens upstream via Redis Pub/Sub.
 *
 * <p>Thread-safety: {@link ConcurrentHashMap} of synchronized session sets.
 * Sets are intentionally allowed to be momentarily empty before being removed
 * — the {@code computeIfPresent} on unregister handles that race.
 */
@Component
public class LocalSessionRegistry implements SessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(LocalSessionRegistry.class);

    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    public LocalSessionRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        meterRegistry.gauge("notification.ws.users.connected.local",
                sessionsByUser, ConcurrentHashMap::size);
    }

    public void register(UUID userId, WebSocketSession session) {
        sessionsByUser
                .computeIfAbsent(userId, k -> Collections.synchronizedSet(new java.util.HashSet<>()))
                .add(session);
        log.debug("Session registered userId={} sessionId={}", userId, session.getId());
    }

    public void unregister(UUID userId, WebSocketSession session) {
        sessionsByUser.computeIfPresent(userId, (k, set) -> {
            set.remove(session);
            return set.isEmpty() ? null : set;
        });
        log.debug("Session unregistered userId={} sessionId={}", userId, session.getId());
    }

    @Override
    public boolean deliver(UUID userId, String payloadJson) {
        Set<WebSocketSession> set = sessionsByUser.get(userId);
        if (set == null || set.isEmpty()) {
            return false;
        }

        TextMessage frame = new TextMessage(payloadJson);
        boolean delivered = false;

        // Snapshot to avoid CME if a session closes mid-iteration.
        WebSocketSession[] snapshot;
        synchronized (set) {
            snapshot = set.toArray(new WebSocketSession[0]);
        }

        for (WebSocketSession s : snapshot) {
            if (!s.isOpen()) {
                unregister(userId, s);
                continue;
            }
            // Per-session lock: concurrent sends to the SAME session can
            // corrupt the WebSocket frame stream. Different sessions still
            // proceed in parallel because the lock is the session itself.
            try {
                synchronized (s) {
                    s.sendMessage(frame);
                }
                delivered = true;
            } catch (IOException | IllegalStateException e) {
                log.warn("WS send failed — closing session userId={} sessionId={}",
                        userId, s.getId(), e);
                try { s.close(); } catch (IOException ignore) { /* already broken */ }
                unregister(userId, s);
            }
        }
        return delivered;
    }

    @Override
    public int localSessionCount(UUID userId) {
        Set<WebSocketSession> set = sessionsByUser.get(userId);
        return set == null ? 0 : set.size();
    }

    /**
     * Package-visible view used by {@link WebSocketHeartbeat} to ping every
     * live session. Returned map is the live backing map — callers must treat
     * it as read-only and synchronise on each value set before iterating.
     */
    java.util.Map<UUID, Set<WebSocketSession>> sessionsView() {
        return sessionsByUser;
    }
}
