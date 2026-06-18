package com.rideflow.notification.infrastructure.cache.redis;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.time.Duration;

/**
 * Wires the Redis backplane used for cluster-wide WebSocket fan-out.
 *
 * <p>The connection points at the shared cache Redis instance (separate from
 * the geo Redis used by location-service). Pub/Sub load is bounded by
 * <em>connected user count × event rate</em> — at portfolio scale this is
 * trivial; even at production scale it would dwarf neither geo writes nor
 * cache hits.
 *
 * <p>The {@link RedisMessageListenerContainer} runs a dedicated subscriber
 * thread that <b>cannot</b> share the same Lettuce connection used by
 * {@link StringRedisTemplate}: a subscribed connection is in a different
 * protocol mode and cannot serve normal commands. We let Lettuce handle that
 * separation internally.
 */
@Configuration
public class RedisConfig {

    @Bean(name = "pubsubConnectionFactory", destroyMethod = "destroy")
    public RedisConnectionFactory pubsubConnectionFactory(
            @Value("${rideflow.redis.pubsub.host}") String host,
            @Value("${rideflow.redis.pubsub.port}") int port,
            @Value("${rideflow.redis.pubsub.database}") int database,
            @Value("${rideflow.redis.pubsub.timeout}") Duration timeout) {

        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(host, port);
        standalone.setDatabase(database);

        LettuceClientConfiguration client = LettuceClientConfiguration.builder()
                .commandTimeout(timeout)
                .clientOptions(ClientOptions.builder()
                        .autoReconnect(true)
                        .socketOptions(SocketOptions.builder()
                                .connectTimeout(timeout)
                                .keepAlive(true)
                                .build())
                        .build())
                .build();

        LettuceConnectionFactory f = new LettuceConnectionFactory(standalone, client);
        f.setValidateConnection(false);
        return f;
    }

    @Bean(name = "pubsubRedisTemplate")
    public StringRedisTemplate pubsubRedisTemplate(RedisConnectionFactory cf) {
        StringRedisTemplate t = new StringRedisTemplate();
        t.setConnectionFactory(cf);
        t.afterPropertiesSet();
        return t;
    }

    /**
     * The listener container is started by Spring; individual subscribers
     * register patterns at construction. The container holds its own subscribe
     * connection independent of the template.
     */
    @Bean(destroyMethod = "stop")
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory cf) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(cf);
        return container;
    }
}
