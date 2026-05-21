package com.hospital.auth.config;

import com.hospital.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ============================================================================
 * Security Configuration
 * ============================================================================
 * Central Spring Security configuration for the Auth Service. This class
 * defines the security filter chain, authentication manager, and password
 * encoder beans.
 *
 * Security design decisions:
 * - Stateless sessions (no server-side session storage) — JWT handles state
 * - CSRF disabled — safe because we use JWT tokens, not cookies
 * - /auth/** endpoints are public (registration, login, token validation)
 * - /actuator/** endpoints are public (for monitoring and health checks)
 * - All other endpoints require authentication
 * - JWT filter runs before UsernamePasswordAuthenticationFilter to intercept
 *   and validate Bearer tokens before standard Spring Security processing
 *
 * @EnableMethodSecurity allows using @PreAuthorize annotations on controller
 * methods for fine-grained role-based access control.
 * ============================================================================
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Configures the security filter chain with stateless JWT-based authentication.
     *
     * @param http the HttpSecurity builder
     * @return the configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF protection — not needed for stateless JWT authentication
                .csrf(AbstractHttpConfigurer::disable)

                // Configure URL-based authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints: authentication and actuator
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Use stateless sessions — no server-side session storage
                // Each request must carry its own JWT token for authentication
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Register the JWT filter to run before the standard authentication filter
                // This ensures Bearer tokens are processed before any form-based auth
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Exposes the AuthenticationManager bean for use in the AuthService.
     * The AuthenticationManager delegates to the CustomUserDetailsService
     * and PasswordEncoder for credential verification during login.
     *
     * @param authenticationConfiguration Spring's authentication configuration
     * @return the configured AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Provides a BCryptPasswordEncoder bean for hashing and verifying passwords.
     *
     * BCrypt automatically handles:
     * - Salt generation (unique per hash)
     * - Configurable work factor (default: 10 rounds)
     * - Constant-time comparison to prevent timing attacks
     *
     * @return a BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
