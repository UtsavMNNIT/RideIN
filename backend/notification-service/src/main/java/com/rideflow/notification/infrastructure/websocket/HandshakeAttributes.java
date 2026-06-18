package com.rideflow.notification.infrastructure.websocket;

/** Keys for attributes attached to a {@code WebSocketSession} at handshake time. */
public final class HandshakeAttributes {

    private HandshakeAttributes() {}

    public static final String ATTR_USER_ID = "rideflow.userId";
    public static final String ATTR_ROLE    = "rideflow.role";
}
