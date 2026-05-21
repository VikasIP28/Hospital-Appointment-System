package com.hospital.appointment.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * Global Exception Handler — Appointment Service
 * ============================================================================
 * Centralized exception handling for all REST controllers. Converts every
 * known exception type into a consistent ErrorResponse JSON structure with
 * the appropriate HTTP status code.
 *
 * Handled exception categories:
 * - Custom business exceptions (404, 400)
 * - Validation errors (400)
 * - Resilience4j: CircuitBreaker open (503), Timeout (504)
 * - Kafka failures (500)
 * - Generic / unexpected (500)
 * ============================================================================
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles AppointmentNotFoundException → 404 Not Found.
     */
    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAppointmentNotFound(AppointmentNotFoundException ex) {
        log.warn("Appointment not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles InvalidStatusTransitionException → 400 Bad Request.
     */
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        log.warn("Invalid status transition: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles Jakarta Bean Validation failures → 400 Bad Request.
     * Collects all field-level errors into a single comma-separated message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", errors);
        return buildResponse(HttpStatus.BAD_REQUEST, errors);
    }

    /**
     * Handles Resilience4j CircuitBreaker open state → 503 Service Unavailable.
     * This is thrown when the circuit breaker is OPEN and further calls
     * to Doctor Service are temporarily blocked.
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        log.error("Circuit breaker is OPEN — Doctor Service unavailable: {}", ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "Doctor Service is temporarily unavailable. Circuit breaker is OPEN. Please try again later.");
    }

    /**
     * Handles timeout exceptions → 504 Gateway Timeout.
     * Thrown when TimeLimiter cuts off a long-running Doctor Service call.
     */
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(TimeoutException ex) {
        log.error("Request timed out: {}", ex.getMessage());
        return buildResponse(HttpStatus.GATEWAY_TIMEOUT,
                "Request to Doctor Service timed out. Please try again later.");
    }

    /**
     * Handles Kafka-related exceptions → 500 Internal Server Error.
     */
    @ExceptionHandler(KafkaException.class)
    public ResponseEntity<ErrorResponse> handleKafkaException(KafkaException ex) {
        log.error("Kafka error occurred: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Event publishing failed. Please try again later.");
    }

    /**
     * Catch-all handler for any unhandled exceptions → 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred: " + ex.getMessage());
    }

    /**
     * Helper to build a consistent ErrorResponse entity.
     */
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        ErrorResponse error = ErrorResponse.builder()
                .status(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, status);
    }
}
