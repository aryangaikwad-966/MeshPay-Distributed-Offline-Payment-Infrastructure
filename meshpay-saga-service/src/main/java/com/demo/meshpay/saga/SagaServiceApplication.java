package com.demo.meshpay.saga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * Saga Service Application
 * Orchestrates distributed payment transactions using the Saga pattern
 */
@SpringBootApplication
@EnableEurekaClient
public class SagaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SagaServiceApplication.class, args);
    }
}
