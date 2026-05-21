package com.hospital.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

/**
 * CORS (Cross-Origin Resource Sharing) Configuration
 * ====================================================
 * Configures CORS policies for the API Gateway.
 *
 * In DEVELOPMENT mode, this configuration is permissive:
 *   - Allows ALL origins (any frontend can call the API)
 *   - Allows ALL HTTP methods (GET, POST, PUT, DELETE, PATCH, OPTIONS)
 *   - Allows ALL headers (Authorization, Content-Type, etc.)
 *   - Allows credentials (cookies, authorization headers)
 *   - Pre-flight cache max age: 3600 seconds (1 hour)
 *
 * WARNING: In PRODUCTION, you should restrict allowed origins to your
 * specific frontend domains (e.g., "https://hospital-app.example.com").
 *
 * This CorsFilter bean is registered at the servlet level, ensuring
 * CORS headers are added BEFORE Spring Security processes the request.
 * This is important because browsers send a pre-flight OPTIONS request
 * that must be handled before authentication kicks in.
 */
@Configuration
@Slf4j
public class CorsConfig {

    /**
     * Creates a CorsFilter bean with permissive CORS settings for development.
     * The filter is applied to all URL patterns (/**).
     *
     * @return configured CorsFilter
     */
    @Bean
    public CorsFilter corsFilter() {
        log.info("Initializing CORS configuration for API Gateway (development mode - all origins allowed)");

        CorsConfiguration corsConfiguration = new CorsConfiguration();

        // Allow all origins - RESTRICT THIS IN PRODUCTION
        corsConfiguration.setAllowedOriginPatterns(Collections.singletonList("*"));

        // Allow all standard HTTP methods used by REST APIs
        corsConfiguration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Allow all headers - especially Authorization for JWT and Content-Type for JSON
        corsConfiguration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Expose response headers that the browser should be able to access
        corsConfiguration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition"
        ));

        // Allow credentials (cookies, authorization headers, TLS client certificates)
        corsConfiguration.setAllowCredentials(true);

        // Cache pre-flight response for 1 hour to reduce OPTIONS requests
        corsConfiguration.setMaxAge(3600L);

        // Apply this CORS configuration to ALL URL patterns
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsFilter(source);
    }
}
