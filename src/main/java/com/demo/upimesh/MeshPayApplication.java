package com.demo.upimesh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the MeshPay Payment Service.
 * This is the Payment microservice in the MeshPay architecture.
 *
 * Run from terminal:
 *   ./mvnw spring-boot:run        (Linux/Mac)
 *   mvnw.cmd spring-boot:run      (Windows)
 *
 * Then open http://localhost:8081
 */
@SpringBootApplication
@EnableScheduling
@EnableEurekaClient
public class MeshPayApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeshPayApplication.class, args);
    }
}
