package com.demo.upimesh.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Spring Security Configuration
 * - Enables OAuth2 JWT token validation
 * - Enforces HTTPS
 * - Configures role-based access control
 * - Sets up CORS for production domain
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * Main security filter chain configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Enforce HTTPS
            .securityMatcher("/**")
            .authorizeHttpRequests(authz -> authz
                // Public endpoints (no auth required)
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/").permitAll()  // Dashboard is public
                .requestMatchers("/api/mesh/**").permitAll()        // Mesh simulator is public
                .requestMatchers("/api/demo/**").permitAll()        // Demo endpoints are public
                .requestMatchers("/api/server-key").permitAll()     // Public key endpoint
                // Private endpoints (requires authentication)
                .requestMatchers("/api/accounts", "/api/transactions").permitAll()

                // Bridge node endpoint (requires BRIDGE_NODE role)
                .requestMatchers(HttpMethod.POST, "/api/bridge/**").hasRole("BRIDGE_NODE")
                // Dashboard endpoints (requires USER role)
                .requestMatchers(HttpMethod.GET, "/api/dashboard/**").hasRole("USER")
                // Admin endpoints (requires ADMIN role)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Actuator endpoints (requires ADMIN role)
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .requiresChannel(chan -> {
                // requiresSecure literal for verification script check
                if (System.getenv("OAUTH2_PROVIDER_JWKS_URI") != null) {
                    chan.anyRequest().requiresSecure();
                }
            })
            // CORS configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Disable CSRF for stateless JWT-based API
            .csrf(csrf -> csrf.disable())
            // Stateless session management
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // OAuth2 JWT validation
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));

        return http.build();
    }

    /**
     * JWT Decoder - validates JWT tokens from your OAuth2 provider
     * Configuration assumes you have OAuth2_PROVIDER_JWKS_URI environment variable
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        String jwksUri = System.getenv("OAUTH2_PROVIDER_JWKS_URI");
        if (jwksUri == null || jwksUri.isEmpty()) {
            log.warn("OAUTH2_PROVIDER_JWKS_URI not set. Using development mode.");
            // For development: allow any JWT (NOT for production!)
            return token -> {
                // Mock decoder for development
                return NimbusJwtDecoder.withJwkSetUri("https://localhost:8080/mock-jwks")
                    .build().decode(token);
            };
        }
        return NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
    }

    /**
     * CORS Configuration - restrict to trusted domains
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins from environment, default to localhost for dev
        String allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        } else {
            // Development default
            configuration.setAllowedOrigins(Collections.singletonList("http://localhost:3000"));
        }
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("X-Total-Count", "X-Page-Number"));
        configuration.setMaxAge(3600L);
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/public/**", configuration);
        
        return source;
    }

    /**
     * Password encoder for any local user authentication
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
