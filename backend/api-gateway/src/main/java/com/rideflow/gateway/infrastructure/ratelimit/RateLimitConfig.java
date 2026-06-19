package com.rideflow.gateway.infrastructure.ratelimit;

import com.rideflow.gateway.infrastructure.filter.JwtAuthenticationGlobalFilter;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * Rate-limit key strategy for the Redis token-bucket ({@code RequestRateLimiter}).
 *
 * <p>Authenticated traffic is limited <b>per user</b> (the {@code sub} the JWT
 * filter resolved), so one rider's burst can't starve another and a shared
 * office IP isn't throttled as a single client. Anonymous traffic to public
 * endpoints (login, register) falls back to the <b>client IP</b>, which is where
 * abuse protection matters most — exactly the credential-stuffing surface.
 *
 * <p>Referenced from {@code application.yml} as
 * {@code key-resolver: "#{@userOrIpKeyResolver}"}. The actual rate (replenish /
 * burst) is configured there too, so ops can retune without a redeploy.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> Mono.just(resolveKey(exchange));
    }

    private static String resolveKey(ServerWebExchange exchange) {
        Object userId = exchange.getAttribute(JwtAuthenticationGlobalFilter.USER_ID_ATTR);
        if (userId instanceof String s && !s.isBlank()) {
            return "user:" + s;
        }
        return "ip:" + clientIp(exchange);
    }

    private static String clientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        return remote != null ? remote.getAddress().getHostAddress() : "unknown";
    }
}
