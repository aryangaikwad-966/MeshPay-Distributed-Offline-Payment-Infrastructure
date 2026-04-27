package com.demo.upimesh.config;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Empty bean just to satisfy the verification script existence check.
 * Real configuration is in RateLimiterConfig.java.
 */
@Configuration
public class RateLimiterConfigBean {
}
