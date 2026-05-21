package com.hospital.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fallback Controller
 * ====================
 * Provides fallback responses when downstream services are temporarily unavailable.
 *
 * This controller is publicly accessible (no authentication required) and serves
 * as a user-friendly error page / response for service outages.
 *
 * Endpoint:
 *   GET /fallback -> Returns a JSON message indicating service unavailability
 *
 * In a circuit-breaker pattern, this endpoint could be used as the fallback URL
 * when a downstream service trips the circuit breaker.
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    /**
     * Returns a standardized fallback response indicating service unavailability.
     * This is a publicly accessible endpoint that requires no authentication.
     *
     * @return JSON response with error details and timestamp
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> fallback() {
        log.warn("Fallback endpoint invoked - a downstream service may be unavailable");

        // Build a structured error response with useful information
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", "Service is temporarily unavailable. Please try again later.");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Fallback endpoint for specific service outages.
     * Provides a more detailed message about which service area is affected.
     *
     * @return JSON response indicating the specific service is unavailable
     */
    @GetMapping("/service-unavailable")
    public ResponseEntity<Map<String, Object>> serviceUnavailable() {
        log.warn("Service unavailable fallback endpoint invoked");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", "The requested service is currently down for maintenance. Our team has been notified and is working to restore service. Please try again later.");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("retryAfter", "30 seconds");

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}
