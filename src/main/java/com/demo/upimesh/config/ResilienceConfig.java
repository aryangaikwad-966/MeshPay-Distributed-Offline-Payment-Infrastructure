package com.demo.upimesh.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j Circuit Breaker Configuration
 * Configures circuit breakers, retry, and time limiter for external service calls
 */
@Configuration
public class ResilienceConfig {

    /**
     * Circuit Breaker for Payment Settlement Service
     */
    @Bean
    public CircuitBreaker paymentSettlementCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit if 50% of calls fail
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before attempting to close
                .slidingWindowSize(10) // Consider last 10 calls
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(5) // Minimum calls before calculating failure rate
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 calls in half-open state
                .slowCallRateThreshold(50) // Open if 50% of calls are slow
                .slowCallDurationThreshold(Duration.ofSeconds(5)) // Calls slower than 5s are considered slow
                .build();

        return registry.circuitBreaker("paymentSettlement", config);
    }

    /**
     * Circuit Breaker for Kafka Producer
     */
    @Bean
    public CircuitBreaker kafkaProducerCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(60)
                .waitDurationInOpenState(Duration.ofSeconds(20))
                .slidingWindowSize(20)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(10)
                .permittedNumberOfCallsInHalfOpenState(5)
                .build();

        return registry.circuitBreaker("kafkaProducer", config);
    }

    /**
     * Circuit Breaker for Outbox Event Processor
     */
    @Bean
    public CircuitBreaker outboxProcessorCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(40)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowSize(50)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(20)
                .permittedNumberOfCallsInHalfOpenState(10)
                .build();

        return registry.circuitBreaker("outboxProcessor", config);
    }

    /**
     * Retry Configuration for Payment Settlement
     */
    @Bean
    public Retry paymentSettlementRetry(RetryRegistry registry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1000))
                .retryOnException(e -> e instanceof RuntimeException)
                .ignoreExceptions(IllegalArgumentException.class, IllegalStateException.class)
                .build();

        return registry.retry("paymentSettlement", config);
    }

    /**
     * Time Limiter for Payment Settlement
     */
    @Bean
    public TimeLimiter paymentSettlementTimeLimiter(TimeLimiterRegistry registry) {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .cancelRunningFuture(true)
                .build();

        return registry.timeLimiter("paymentSettlement", config);
    }
}
