package com.rideflow.gateway.infrastructure.security;

import com.rideflow.gateway.infrastructure.config.GatewayProperties;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.RequestPath;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

/**
 * Decides whether a request may bypass JWT authentication. Backed by the
 * configurable {@code rideflow.gateway.public-endpoints} list (login, register,
 * public rate cards, health, …). CORS preflight ({@code OPTIONS}) is always
 * public so browsers can negotiate before they hold a token.
 */
@Component
public class PublicEndpointMatcher {

    private final List<Rule> rules;

    public PublicEndpointMatcher(GatewayProperties props) {
        PathPatternParser parser = PathPatternParser.defaultInstance;
        this.rules = props.publicEndpoints().stream()
                .map(pe -> new Rule(toMethod(pe.method()), parser.parse(pe.pattern())))
                .toList();
    }

    public boolean isPublic(HttpMethod method, RequestPath path) {
        if (HttpMethod.OPTIONS.equals(method)) {
            return true;   // CORS preflight
        }
        for (Rule rule : rules) {
            if ((rule.method() == null || rule.method().equals(method))
                    && rule.pattern().matches(path.pathWithinApplication())) {
                return true;
            }
        }
        return false;
    }

    private static HttpMethod toMethod(String method) {
        return (method == null || method.isBlank()) ? null : HttpMethod.valueOf(method.trim().toUpperCase());
    }

    private record Rule(HttpMethod method, PathPattern pattern) {}
}
