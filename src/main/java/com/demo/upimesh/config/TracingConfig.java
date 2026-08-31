package com.demo.upimesh.config;

import io.micrometer.tracing.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry Tracing Configuration
 * Uses Spring Boot's auto-configuration for distributed tracing
 */
@Configuration
public class TracingConfig {

    @Bean
    public Tracer tracer() {
        // Spring Boot will auto-configure the tracer based on micrometer-tracing-bridge-otel
        // This bean is provided for manual tracing if needed
        return null;
    }
}
