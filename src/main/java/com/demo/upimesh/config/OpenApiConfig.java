package com.demo.upimesh.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger 3.0) Configuration
 * Generates API documentation automatically from controller annotations
 * 
 * Available at: http://localhost:8080/swagger-ui.html
 * OpenAPI JSON: http://localhost:8080/v3/api-docs
 */
@Slf4j
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MeshPay API")
                .version("1.0.0")
                .description("Offline UPI payments via Bluetooth mesh network. " +
                           "Enables payments in areas with zero internet connectivity.")
                .contact(new Contact()
                    .name("UPI Mesh Team")
                    .email("support@upimesh.example.com")
                    .url("https://upimesh.example.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT bearer token for API authentication")))
            .addSecurityItem(new SecurityRequirement()
                .addList("bearerAuth"));
    }
}
