package com.rideflow.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * RideFlow API gateway — the single ingress for the platform.
 *
 * <p>Built on Spring Cloud Gateway (reactive / Netty). It is the only component
 * that validates JWTs: it authenticates each request, injects trusted identity
 * headers ({@code X-User-*}) that the downstream services rely on, rate-limits
 * per caller, terminates CORS, and emits one structured access log per request.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
