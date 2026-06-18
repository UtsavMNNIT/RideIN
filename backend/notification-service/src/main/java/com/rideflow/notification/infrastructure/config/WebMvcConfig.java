package com.rideflow.notification.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS for HTTP endpoints. The WebSocket handshake has its own
 * allowed-origins config in {@code WebSocketConfig}; the two settings are
 * intentionally separate because Spring serves them through different stacks.
 *
 * <p>Production should narrow {@code allowed-origins} to the Vercel domain.
 * The default {@code *} works for local dev where the frontend runs at
 * {@code http://localhost:3000}.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String allowedOrigins;

    public WebMvcConfig(@Value("${rideflow.cors.allowed-origins:*}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // allowedOriginPatterns (not allowedOrigins) is required when we want
        // to allow `*` together with credentials in newer Spring versions.
        registry.addMapping("/v1/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
