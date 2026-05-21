package com.hospital.gateway.config;

import com.hospital.gateway.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration
 * =======================
 * Configures Spring Security for the API Gateway with the following policies:
 *
 * PUBLIC endpoints (no authentication required):
 *   - /api/auth/**    : Login, registration, and token refresh
 *   - /actuator/**    : Health checks and metrics for monitoring
 *   - /fallback/**    : Fallback responses when services are unavailable
 *
 * PROTECTED endpoints (JWT authentication required):
 *   - Everything else : All proxied API requests need a valid JWT
 *
 * Key design decisions:
 *   - CSRF is disabled because the gateway is a stateless REST API using JWT tokens
 *   - Sessions are STATELESS - no server-side session is maintained
 *   - The JWT filter runs before Spring's UsernamePasswordAuthenticationFilter
 *   - Method-level security is enabled for fine-grained role checks (if needed)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Defines the HTTP security filter chain.
     * This is the primary security configuration that controls access to all endpoints.
     *
     * @param http the HttpSecurity builder
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring API Gateway security filter chain");

        http
                // Disable CSRF - not needed for stateless JWT-based authentication
                .csrf(AbstractHttpConfigurer::disable)

                // Configure endpoint authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - accessible without authentication
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/fallback/**").permitAll()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Use stateless session management - no HTTP sessions are created or used
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Add the JWT filter before the default username/password authentication filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
