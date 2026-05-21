package com.hospital.appointment.config;

import com.hospital.appointment.security.JwtAuthenticationFilter;
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
 * Spring Security configuration for the Appointment Service.
 *
 * Configures stateless JWT-based authentication with role-based access control:
 * - Actuator endpoints are publicly accessible for monitoring tools
 * - POST /appointments requires PATIENT role
 * - Confirm/reject endpoints require DOCTOR or ADMIN role
 * - Admin endpoints require ADMIN role
 * - All other endpoints require authentication
 *
 * The JWT filter is inserted before the UsernamePasswordAuthenticationFilter
 * in the filter chain.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Configures the HTTP security filter chain.
     *
     * @param http the HttpSecurity builder
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF since we use stateless JWT tokens
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session management - no HTTP sessions
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Actuator endpoints - open for monitoring tools (Prometheus, etc.)
                        .requestMatchers("/actuator/**").permitAll()

                        // POST /appointments - only patients can create appointments
                        .requestMatchers(HttpMethod.POST, "/appointments").hasRole("PATIENT")

                        // Confirm and reject - only doctors or admins
                        .requestMatchers(HttpMethod.PUT, "/appointments/*/confirm")
                        .hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/appointments/*/reject")
                        .hasAnyRole("DOCTOR", "ADMIN")

                        // Admin endpoints - only admins
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Insert JWT filter before the standard authentication filter
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
