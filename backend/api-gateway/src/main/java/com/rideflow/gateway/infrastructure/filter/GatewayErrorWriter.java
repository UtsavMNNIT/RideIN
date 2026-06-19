package com.rideflow.gateway.infrastructure.filter;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Writes a uniform JSON error body and short-circuits the exchange — the
 * reactive equivalent of the servlet services' {@code ApiError}. Used by the
 * gateway's own filters (e.g. a 401 from JWT auth) so clients get a consistent
 * error shape whether the failure originates at the edge or downstream.
 */
final class GatewayErrorWriter {

    private GatewayErrorWriter() {}

    static Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getPath().value();
        String body = """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s"}"""
                .formatted(Instant.now(), status.value(), status.getReasonPhrase(),
                        escape(message), escape(path));

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
