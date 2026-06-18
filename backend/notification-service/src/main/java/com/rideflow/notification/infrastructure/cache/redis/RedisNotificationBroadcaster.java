package com.rideflow.notification.infrastructure.cache.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.notification.application.port.out.NotificationBroadcaster;
import com.rideflow.notification.domain.model.Notification;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes notifications onto a per-user Redis Pub/Sub channel
 * ({@code notify:user:<userId>}). All notification-service replicas subscribe
 * to {@code notify:user:*} via {@link RedisNotificationSubscriber}; only the
 * replica(s) that hold a local WebSocket session for the recipient actually
 * write to a socket.
 *
 * <p><b>Why per-user channels, not one global channel?</b> Server-side
 * filtering: Redis only sends a message to subscribers whose pattern matches.
 * With a single channel, every replica would receive every notification and
 * filter in-process — wasted bandwidth proportional to N × event-rate.
 */
@Component
public class RedisNotificationBroadcaster implements NotificationBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(RedisNotificationBroadcaster.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper        objectMapper;
    private final String              channelPrefix;
    private final Counter             publishedCounter;
    private final Counter             failedCounter;

    public RedisNotificationBroadcaster(
            StringRedisTemplate pubsubRedisTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${rideflow.redis.pubsub.channel-prefix}") String channelPrefix) {
        this.redis            = pubsubRedisTemplate;
        this.objectMapper     = objectMapper;
        this.channelPrefix    = channelPrefix;
        this.publishedCounter = meterRegistry.counter("notification.broadcast", "result", "published");
        this.failedCounter    = meterRegistry.counter("notification.broadcast", "result", "failed");
    }

    @Override
    public void broadcast(Notification notification) {
        String channel = channelPrefix + notification.userId();
        String payload;
        try {
            payload = objectMapper.writeValueAsString(notification);
        } catch (JsonProcessingException e) {
            failedCounter.increment();
            throw new IllegalStateException("Failed to serialize notification " + notification.id(), e);
        }

        try {
            redis.convertAndSend(channel, payload);
            publishedCounter.increment();
            log.debug("Broadcast notificationId={} channel={}", notification.id(), channel);
        } catch (RuntimeException e) {
            // Don't propagate — durability lives in PG; reconnects will backfill.
            failedCounter.increment();
            log.warn("Broadcast failed channel={} notificationId={}", channel, notification.id(), e);
        }
    }
}
