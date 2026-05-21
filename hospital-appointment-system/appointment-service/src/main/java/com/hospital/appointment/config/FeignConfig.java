package com.hospital.appointment.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Feign client configuration for inter-service communication.
 *
 * Provides:
 * 1. A request interceptor that propagates the JWT token from the current
 *    SecurityContext to outgoing Feign requests, enabling authenticated
 *    service-to-service calls.
 * 2. A custom error decoder for handling non-2xx responses from
 *    downstream services with appropriate logging.
 */
@Configuration
@Slf4j
public class FeignConfig {

    /**
     * Request interceptor that extracts the JWT token from the current
     * security context and adds it as an Authorization header to all
     * outgoing Feign requests.
     *
     * This ensures that when the Appointment Service calls the Doctor
     * Service, the original user's credentials are forwarded.
     *
     * @return configured RequestInterceptor
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return (RequestTemplate template) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getCredentials() != null) {
                String token = authentication.getCredentials().toString();
                template.header("Authorization", "Bearer " + token);
                log.debug("Propagating JWT token to Feign request: {}", template.url());
            } else {
                log.debug("No JWT token found in SecurityContext for Feign request: {}", template.url());
            }
        };
    }

    /**
     * Custom error decoder that translates non-2xx HTTP responses from
     * downstream services into meaningful exceptions with logging.
     *
     * @return configured ErrorDecoder
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return (String methodKey, feign.Response response) -> {
            log.error("Feign client error: method={}, status={}, reason={}",
                    methodKey, response.status(), response.reason());

            return switch (response.status()) {
                case 404 -> new RuntimeException(
                        "Resource not found in downstream service: " + methodKey);
                case 503 -> new RuntimeException(
                        "Downstream service unavailable: " + methodKey);
                default -> new RuntimeException(
                        "Feign client error: status=" + response.status()
                                + ", method=" + methodKey);
            };
        };
    }
}
