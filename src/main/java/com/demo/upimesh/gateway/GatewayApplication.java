package com.demo.upimesh.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

/**
 * API Gateway for Microservices
 * Single entry point routing requests to appropriate services
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("payment-service", r -> r
                .path("/api/payments/**")
                .uri("lb://payment-service"))
            .route("saga-service", r -> r
                .path("/api/sagas/**")
                .uri("lb://saga-service"))
            .route("mesh-service", r -> r
                .path("/api/mesh/**")
                .uri("lb://payment-service"))
            .build();
    }
}
