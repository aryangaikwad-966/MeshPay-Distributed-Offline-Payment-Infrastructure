package com.demo.upimesh.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate Limiting Configuration using Resilience4j
 * Prevents DOS attacks by limiting request throughput per endpoint
 * 
 * Rate limiters are configured via application.properties:
 * - resilience4j.ratelimiter.configs.default.*
 * - resilience4j.ratelimiter.instances.*
 */
@Configuration
public class RateLimiterConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterConfig.class);

    /**
     * Create a default Rate Limiter Registry
     * Individual limiters can be configured via application.properties or @RateLimiter annotations
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterRegistry registry = RateLimiterRegistry.ofDefaults();
        log.info("RateLimiterRegistry initialized with default configuration");
        return registry;
    }
}
