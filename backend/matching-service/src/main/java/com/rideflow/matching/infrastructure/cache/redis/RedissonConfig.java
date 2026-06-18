package com.rideflow.matching.infrastructure.cache.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the single {@link RedissonClient} used for distributed locking.
 *
 * <h2>Reusing the GEO Redis instance</h2>
 * We point Redisson at the same Redis that holds driver geo-data. Justifications:
 * <ul>
 *   <li>Lock load is tiny — {@code O(active_dispatches)} keys with second-scale
 *       TTLs — compared to geo writes.</li>
 *   <li>Co-locating lock state with candidate-data state removes the failure
 *       mode "Redis-A is up, Redis-B is down, dispatch picks a stale driver
 *       and locks them on a healthy instance".</li>
 *   <li>Portfolio constraint: no extra infra.</li>
 * </ul>
 *
 * <h2>Why not {@code redisson-spring-boot-starter}</h2>
 * The starter auto-configures a {@code RedisConnectionFactory} that competes
 * with the Spring Data Redis {@code StringRedisTemplate} we use elsewhere.
 * Bringing in only {@code org.redisson:redisson} and wiring the client
 * explicitly avoids the clash and keeps connection management
 * single-purpose.
 *
 * <h2>Retry tuning</h2>
 * Redisson defaults to 3 attempts × 1500ms — far too slow for dispatch's
 * sub-second SLO. We tighten to 2 × 100ms: enough to absorb a single TCP
 * blip, not enough to dominate dispatch latency.
 */
@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${rideflow.redis.geo.host}") String host,
            @Value("${rideflow.redis.geo.port}") int port,
            @Value("${rideflow.dispatch.lock.acquire-retry-attempts}") int retryAttempts,
            @Value("${rideflow.dispatch.lock.acquire-retry-interval-millis}") int retryIntervalMs) {

        Config config = new Config();
        config.setCodec(StringCodec.INSTANCE);

        config.useSingleServer()
              .setAddress("redis://" + host + ":" + port)
              .setConnectionPoolSize(16)
              .setConnectionMinimumIdleSize(4)
              .setRetryAttempts(retryAttempts)
              .setRetryInterval(retryIntervalMs)
              .setTimeout(500)
              .setConnectTimeout(1000)
              .setIdleConnectionTimeout(10_000);

        return Redisson.create(config);
    }
}
