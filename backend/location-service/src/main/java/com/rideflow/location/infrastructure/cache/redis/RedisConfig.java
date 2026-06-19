package com.rideflow.location.infrastructure.cache.redis;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;

import java.time.Duration;

/**
 * Wires <b>two</b> independent Redis connections:
 * <ul>
 *   <li>{@code @Primary "geo"} — points at {@code redis-geo}; {@code noeviction};
 *       holds the geo index, metadata, and heartbeat sets.</li>
 *   <li>{@code "cache"} — points at {@code redis-cache}; LRU; placeholder for
 *       rate-limit buckets, idempotency keys, fare cache (used by later slices).</li>
 * </ul>
 *
 * Lettuce is the chosen client (Spring Boot default): netty-based, async,
 * thread-safe single-connection. Connection pooling is unnecessary for the
 * shared-template use-case; Lettuce multiplexes pipelined commands on one
 * connection.
 */
@Configuration
public class RedisConfig {

    // -------------------------------------------------------------------
    // Properties classes
    // -------------------------------------------------------------------

    @ConfigurationProperties(prefix = "rideflow.redis.geo")
    public static class GeoProps   extends RedisEndpointProps {}

    @ConfigurationProperties(prefix = "rideflow.redis.cache")
    public static class CacheProps extends RedisEndpointProps {}

    public static class RedisEndpointProps {
        private String   host;
        private int      port;
        private int      database;
        private Duration timeout = Duration.ofSeconds(1);

        public String   getHost()                  { return host; }
        public void     setHost(String host)       { this.host = host; }
        public int      getPort()                  { return port; }
        public void     setPort(int port)          { this.port = port; }
        public int      getDatabase()              { return database; }
        public void     setDatabase(int d)         { this.database = d; }
        public Duration getTimeout()               { return timeout; }
        public void     setTimeout(Duration t)     { this.timeout = t; }
    }

    @Bean @Primary
    public GeoProps geoProps()       { return new GeoProps(); }

    @Bean
    public CacheProps cacheProps()   { return new CacheProps(); }

    // -------------------------------------------------------------------
    // Connection factories
    // -------------------------------------------------------------------

    @Bean(name = "geoRedisConnectionFactory", destroyMethod = "destroy")
    @Primary
    public RedisConnectionFactory geoRedisConnectionFactory(GeoProps props) {
        return buildFactory(props);
    }

    @Bean(name = "cacheRedisConnectionFactory", destroyMethod = "destroy")
    public RedisConnectionFactory cacheRedisConnectionFactory(CacheProps props) {
        return buildFactory(props);
    }

    private LettuceConnectionFactory buildFactory(RedisEndpointProps p) {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(p.getHost(), p.getPort());
        standalone.setDatabase(p.getDatabase());

        LettuceClientConfiguration client = LettuceClientConfiguration.builder()
                .commandTimeout(p.getTimeout())
                .clientOptions(ClientOptions.builder()
                        .autoReconnect(true)
                        .socketOptions(SocketOptions.builder()
                                .connectTimeout(p.getTimeout())
                                .keepAlive(true)
                                .build())
                        .build())
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone, client);
        factory.setValidateConnection(false);
        return factory;
    }

    // -------------------------------------------------------------------
    // Templates
    // -------------------------------------------------------------------

    @Bean(name = "geoRedisTemplate")
    @Primary
    public StringRedisTemplate geoRedisTemplate(
            @Qualifier("geoRedisConnectionFactory") RedisConnectionFactory cf) {
        StringRedisTemplate t = new StringRedisTemplate();
        t.setConnectionFactory(cf);
        // StringRedisTemplate already defaults every serializer to
        // StringRedisSerializer; set it explicitly for clarity.
        t.setKeySerializer(new StringRedisSerializer());
        t.afterPropertiesSet();
        return t;
    }

    @Bean(name = "cacheRedisTemplate")
    public StringRedisTemplate cacheRedisTemplate(
            @Qualifier("cacheRedisConnectionFactory") RedisConnectionFactory cf) {
        StringRedisTemplate t = new StringRedisTemplate();
        t.setConnectionFactory(cf);
        t.afterPropertiesSet();
        return t;
    }

    // -------------------------------------------------------------------
    // Lua scripts (loaded once; spring-data uses EVALSHA with NOSCRIPT fallback)
    // -------------------------------------------------------------------

    @Bean
    public DefaultRedisScript<Long> updateDriverLocationScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/update-driver-location.lua")));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> evictStaleDriverScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/evict-stale-driver.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
