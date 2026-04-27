package com.demo.upimesh.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * HTTP Client Configuration
 * - Connection pooling via RestTemplate
 * - Timeout management
 * - Request interceptors
 */
@Configuration
public class HttpClientConfig {

    private static final Logger log = LoggerFactory.getLogger(HttpClientConfig.class);

    /**
     * Configure RestTemplate with connection pooling and timeouts
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        log.info("Configuring RestTemplate with 5s connect timeout and 10s read timeout");
        return builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }
}
