package com.rideflow.notification.application.port.out;

import java.util.UUID;

/** Output port for delivering payloads to a user's live WebSocket session(s) on this JVM. */
public interface SessionRegistry {

    boolean deliver(UUID userId, String payloadJson);

    int localSessionCount(UUID userId);
}
