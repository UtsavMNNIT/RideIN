package com.rideflow.notification.infrastructure.cache.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.notification.application.port.out.SessionRegistry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Subscribes (pattern {@code notify:user:*}) and dispatches each message to
 * the local {@link SessionRegistry}. If the user has no session on this JVM,
 * the message is dropped silently — another replica handles it (or none does,
 * in which case the row persisted by the consumer is the fallback).
 *
 * <p>The subscriber callback runs on the listener container's thread; it must
 * be fast and non-blocking. {@link SessionRegistry#deliver(UUID, String)}
 * pushes onto the WebSocket session's outbound buffer (bounded by config);
 * actual socket I/O happens on the container's I/O thread.
 */
@Component
public class RedisNotificationSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisNotificationSubscriber.class);

    private final RedisMessageListenerContainer container;
    private final SessionRegistry               sessions;
    private final ObjectMapper                  objectMapper;
    private final String                        channelPattern;
    private final String                        channelPrefix;
    private final Counter                       deliveredCounter;
    private final Counter                       skippedCounter;
    private final Counter                       malformedCounter;

    public RedisNotificationSubscriber(
            RedisMessageListenerContainer container,
            SessionRegistry sessions,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${rideflow.redis.pubsub.channel-pattern}") String channelPattern,
            @Value("${rideflow.redis.pubsub.channel-prefix}")  String channelPrefix) {
        this.container        = container;
        this.sessions         = sessions;
        this.objectMapper     = objectMapper;
        this.channelPattern   = channelPattern;
        this.channelPrefix    = channelPrefix;
        this.deliveredCounter = meterRegistry.counter("notification.receive", "result", "delivered_local");
        this.skippedCounter   = meterRegistry.counter("notification.receive", "result", "no_local_session");
        this.malformedCounter = meterRegistry.counter("notification.receive", "result", "malformed");
    }

    @PostConstruct
    void register() {
        container.addMessageListener(this, new PatternTopic(channelPattern));
        log.info("Subscribed to Redis pattern {}", channelPattern);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body    = new String(message.getBody(),    StandardCharsets.UTF_8);

        UUID userId = extractUserId(channel);
        if (userId == null) {
            malformedCounter.increment();
            return;
        }

        // Validate payload is JSON-parseable before pushing to the socket —
        // a malformed payload should not poison a client's WebSocket frame.
        try {
            JsonNode parsed = objectMapper.readTree(body);
            if (parsed == null || parsed.isMissingNode()) throw new IllegalArgumentException("empty");
        } catch (Exception e) {
            malformedCounter.increment();
            log.warn("Dropping malformed pubsub payload channel={}", channel, e);
            return;
        }

        if (sessions.deliver(userId, body)) {
            deliveredCounter.increment();
        } else {
            skippedCounter.increment();
        }
    }

    private UUID extractUserId(String channel) {
        if (channel == null || !channel.startsWith(channelPrefix)) {
            return null;
        }
        try {
            return UUID.fromString(channel.substring(channelPrefix.length()));
        } catch (IllegalArgumentException e) {
            log.warn("Pubsub channel suffix is not a UUID: {}", channel);
            return null;
        }
    }
}
