package com.hospital.appointment.controller;

import com.hospital.appointment.kafka.producer.AppointmentEventProducer;
import com.hospital.appointment.service.AppointmentService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * Admin Controller — Appointment Service
 * ============================================================================
 * Administrative endpoints for system monitoring, analytics, and event
 * inspection. All endpoints are restricted to users with the ADMIN role.
 *
 * Endpoints:
 *   GET /admin/system/health            — Service health information
 *   GET /admin/system/circuit-breakers  — Resilience4j circuit breaker states
 *   GET /admin/system/retries           — Retry configuration and metrics
 *   GET /admin/analytics/appointments   — Appointment statistics
 *   GET /admin/kafka/events             — Recent Kafka event log
 * ============================================================================
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AppointmentService appointmentService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final AppointmentEventProducer eventProducer;

    /**
     * Returns basic service health information.
     */
    @GetMapping("/system/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        log.info("GET /admin/system/health — checking system health");

        Map<String, Object> health = new HashMap<>();
        health.put("service", "appointment-service");
        health.put("status", "UP");
        health.put("timestamp", java.time.LocalDateTime.now().toString());

        // Include circuit breaker summary
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            Map<String, Object> cbInfo = new HashMap<>();
            cbInfo.put("state", cb.getState().name());
            cbInfo.put("failureRate", cb.getMetrics().getFailureRate());
            cbInfo.put("numberOfBufferedCalls", cb.getMetrics().getNumberOfBufferedCalls());
            health.put("circuitBreaker_" + cb.getName(), cbInfo);
        });

        return ResponseEntity.ok(health);
    }

    /**
     * Returns detailed circuit breaker states for all registered instances.
     * Shows state (CLOSED/OPEN/HALF_OPEN), failure rates, and call counts.
     */
    @GetMapping("/system/circuit-breakers")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStates() {
        log.info("GET /admin/system/circuit-breakers — fetching circuit breaker states");

        Map<String, Object> result = new HashMap<>();

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            CircuitBreaker.Metrics metrics = cb.getMetrics();

            Map<String, Object> cbState = new HashMap<>();
            cbState.put("state", cb.getState().name());
            cbState.put("failureRate", metrics.getFailureRate() + "%");
            cbState.put("slowCallRate", metrics.getSlowCallRate() + "%");
            cbState.put("numberOfBufferedCalls", metrics.getNumberOfBufferedCalls());
            cbState.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
            cbState.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
            cbState.put("numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls());
            cbState.put("numberOfSlowCalls", metrics.getNumberOfSlowCalls());

            result.put(cb.getName(), cbState);
        });

        if (result.isEmpty()) {
            result.put("message", "No circuit breakers registered yet. Make a call to Doctor Service first.");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Returns retry configuration and metrics for all registered retry instances.
     */
    @GetMapping("/system/retries")
    public ResponseEntity<Map<String, Object>> getRetryMetrics() {
        log.info("GET /admin/system/retries — fetching retry metrics");

        Map<String, Object> result = new HashMap<>();

        retryRegistry.getAllRetries().forEach(retry -> {
            Map<String, Object> retryInfo = new HashMap<>();
            retryInfo.put("name", retry.getName());
            retryInfo.put("numberOfSuccessfulCallsWithoutRetryAttempt",
                    retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
            retryInfo.put("numberOfSuccessfulCallsWithRetryAttempt",
                    retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
            retryInfo.put("numberOfFailedCallsWithoutRetryAttempt",
                    retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());
            retryInfo.put("numberOfFailedCallsWithRetryAttempt",
                    retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());

            result.put(retry.getName(), retryInfo);
        });

        if (result.isEmpty()) {
            result.put("message", "No retry instances registered yet. Make a call to Doctor Service first.");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Returns aggregated appointment statistics for the admin dashboard.
     */
    @GetMapping("/analytics/appointments")
    public ResponseEntity<Map<String, Object>> getAppointmentStatistics() {
        log.info("GET /admin/analytics/appointments — fetching appointment statistics");
        Map<String, Object> stats = appointmentService.getAppointmentStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Returns the most recent Kafka events published by this service.
     * The event buffer stores the last 100 events in memory.
     */
    @GetMapping("/kafka/events")
    public ResponseEntity<Map<String, Object>> getKafkaEvents() {
        log.info("GET /admin/kafka/events — fetching recent Kafka events");

        Map<String, Object> result = new HashMap<>();
        var events = eventProducer.getRecentEvents();
        result.put("totalRecentEvents", events.size());
        result.put("events", events);

        return ResponseEntity.ok(result);
    }
}
