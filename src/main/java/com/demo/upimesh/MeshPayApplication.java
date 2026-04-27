package com.demo.upimesh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the MeshPay encrypted peer-to-peer payment network demo.
 *
 * Run from terminal:
 *   ./mvnw spring-boot:run        (Linux/Mac)
 *   mvnw.cmd spring-boot:run      (Windows)
 *
 * Then open http://localhost:8080
 */
@SpringBootApplication
public class MeshPayApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeshPayApplication.class, args);
    }
}
