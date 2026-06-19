package com.rideflow.gateway.infrastructure.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * One structured access-log line per request, plus correlation-id propagation.
 *
 * <p>Runs <b>first</b> (highest precedence) so it times the entire filter chain
 * including auth and routing. It mints an {@code X-Request-Id} when the client
 * didn't supply one, forwards it downstream, and echoes it on the response so a
 * single id ties together the edge log, every service log, and the client.
 *
 * <p>The log is emitted on completion regardless of outcome (success, error, or
 * cancellation), capturing method, path, resolved route, status, latency, caller
 * IP, and the request id — the minimum a platform on-call needs to triage.
 */
@Component
public class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger("gateway.access");

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        final String rid = requestId;

        ServerHttpRequest mutated = request.mutate()
                .header(REQUEST_ID_HEADER, rid)
                .build();
        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, rid);

        final String method = request.getMethod().name();
        final String path   = request.getURI().getRawPath();
        final String client = clientIp(request);
        final long   start  = System.nanoTime();

        return chain.filter(exchange.mutate().request(mutated).build())
                .doFinally(signal -> {
                    long ms = (System.nanoTime() - start) / 1_000_000;
                    var statusCode = exchange.getResponse().getStatusCode();
                    int status = statusCode != null ? statusCode.value() : 0;
                    Object route = exchange.getAttribute(
                            ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                    String routeId = (route instanceof org.springframework.cloud.gateway.route.Route r)
                            ? r.getId() : "none";

                    log.info("method={} path={} route={} status={} durationMs={} clientIp={} requestId={} outcome={}",
                            method, path, routeId, status, ms, client, rid, signal);
                });
    }

    private static String clientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();   // first hop = original client
        }
        InetSocketAddress remote = request.getRemoteAddress();
        return remote != null ? remote.getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;   // outermost filter — wraps everything
    }
}
