package com.demo.upimesh.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Actuator Configuration for monitoring and metrics
 */
@Configuration
public class ActuatorConfig {

    private static final Logger log = LoggerFactory.getLogger(ActuatorConfig.class);

    /**
     * Meter registry for custom metrics
     * Uses SimpleMeterRegistry as fallback (can be replaced with Prometheus in production)
     */
    @Bean
    public MeterRegistry meterRegistry() {
        MeterRegistry registry = new SimpleMeterRegistry();
        log.info("Meter registry (SimpleMeterRegistry) initialized for metrics");
        return registry;
    }
}
