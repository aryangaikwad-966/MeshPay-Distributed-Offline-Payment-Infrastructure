package com.demo.upimesh.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka Retry Configuration
 * Configures retry behavior for failed message processing
 */
@Configuration
public class KafkaRetryConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        // Configure retry with fixed backoff
        // 3 retries with 1 second backoff between attempts
        FixedBackOff backOff = new FixedBackOff(1000L, 3);
        
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(backOff);
        
        // Configure which exceptions should not be retried
        // These are typically non-transient errors
        errorHandler.addNotRetryableExceptions(
            IllegalArgumentException.class,
            IllegalStateException.class,
            org.springframework.messaging.MessageHandlingException.class
        );
        
        return errorHandler;
    }
}
