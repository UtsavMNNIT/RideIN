package com.rideflow.matching.infrastructure.cache.redis;

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

import java.time.Duration;

/**
 * Lettuce connection to the GEO Redis instance that location-service populates.
 *
 * <p>matching-service is a read-only client of that index — {@code GEOSEARCH}
 * for nearby drivers + pipelined {@code HMGET} on the metadata hashes. (The
 * distributed lock uses a <em>separate</em> Redisson client — see
 * {@code RedissonConfig} — even though it points at the same Redis.)
 */
@Configuration
public class RedisConfig {

    @Bean(name = "geoRedisConnectionFactory", destroyMethod = "destroy")
    public RedisConnectionFactory geoRedisConnectionFactory(
            @Value("${rideflow.redis.geo.host}") String host,
            @Value("${rideflow.redis.geo.port}") int port,
            @Value("${rideflow.redis.geo.database}") int database,
            @Value("${rideflow.redis.geo.timeout}") Duration timeout) {

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

        LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone, client);
        factory.setValidateConnection(false);
        return factory;
    }

    @Bean(name = "geoRedisTemplate")
    public StringRedisTemplate geoRedisTemplate(RedisConnectionFactory geoRedisConnectionFactory) {
        StringRedisTemplate t = new StringRedisTemplate();
        t.setConnectionFactory(geoRedisConnectionFactory);
        t.afterPropertiesSet();
        return t;
    }
}
