package com.hospital.doctor.config;

import com.hospital.doctor.security.JwtAuthenticationFilter;
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
 * ============================================================================
 * Security Configuration
 * ============================================================================
 * Configures Spring Security for the doctor-service microservice.
 *
 * Key decisions:
 * - STATELESS sessions: No server-side session state; JWT tokens carry auth info
 * - CSRF disabled: Not needed for stateless REST APIs using JWT
 * - JWT filter: Added BEFORE UsernamePasswordAuthenticationFilter in the chain
 *
 * Access rules:
 * - /actuator/**            → Permitted (monitoring/health checks)
 * - /doctors/simulate/**    → Permitted (resilience testing endpoints)
 * - /doctors/pending-appointments → Requires DOCTOR or ADMIN role
 * - All other /doctors/**   → Permitted (public doctor directory)
 *
 * Method-level security is enabled via @EnableMethodSecurity, allowing
 * @PreAuthorize annotations on controller methods for fine-grained access control.
 * ============================================================================
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** JWT authentication filter to validate tokens on each request */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Configure the security filter chain with JWT-based authentication.
     *
     * @param http the HttpSecurity builder
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF - not needed for stateless JWT-based REST APIs
                .csrf(AbstractHttpConfigurer::disable)

                // Configure stateless session management - no server-side sessions
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Define URL-based authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Actuator endpoints - permit for monitoring tools (Prometheus, health checks)
                        .requestMatchers("/actuator/**").permitAll()

                        // Simulation endpoints - permit for resilience/chaos testing
                        .requestMatchers("/doctors/simulate/**").permitAll()

                        // Pending appointments endpoint - requires DOCTOR or ADMIN role
                        .requestMatchers("/doctors/pending-appointments")
                            .hasAnyRole("DOCTOR", "ADMIN")

                        // All other doctor endpoints - publicly accessible (doctor directory)
                        .requestMatchers("/doctors/**").permitAll()

                        // Admin endpoints - handled by @PreAuthorize on controller methods
                        .requestMatchers("/admin/**").authenticated()

                        // Any other request requires authentication
                        .anyRequest().authenticated()
                )

                // Add JWT filter before Spring Security's default auth filter
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
