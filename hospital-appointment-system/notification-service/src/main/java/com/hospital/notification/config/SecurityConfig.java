package com.hospital.notification.config;

import com.hospital.notification.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the notification-service.
 *
 * Access rules:
 * - /actuator/** : Permit all (health checks, monitoring)
 * - /admin/**    : Requires ADMIN role (notification management, reports, resend)
 * - /notifications/** : Requires authentication (any authenticated user)
 * - All other endpoints: Require authentication
 *
 * Uses stateless session management (JWT-based, no server-side sessions).
 * CSRF is disabled since the service uses token-based authentication.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF - not needed for stateless JWT authentication
                .csrf(AbstractHttpConfigurer::disable)

                // Configure stateless session management (no server-side session)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Define URL-based authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Actuator endpoints are open for health checks and monitoring
                        .requestMatchers("/actuator/**").permitAll()

                        // Admin endpoints require ADMIN role
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Notification query endpoints require any authenticated user
                        .requestMatchers("/notifications/**").authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // Add JWT filter before Spring Security's default username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
