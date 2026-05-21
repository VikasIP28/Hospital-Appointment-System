package com.hospital.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;

/**
 * Global Exception Handler
 * =========================
 * Catches all unhandled exceptions thrown by the API Gateway controllers
 * and converts them into standardized ErrorResponse JSON objects.
 *
 * This ensures that clients always receive a consistent, machine-parseable
 * error response instead of raw stack traces or Spring's default error pages.
 *
 * Exception handling hierarchy:
 *   1. AccessDeniedException       -> 403 Forbidden
 *   2. AuthenticationException     -> 401 Unauthorized
 *   3. BadCredentialsException     -> 401 Unauthorized
 *   4. ResourceAccessException     -> 503 Service Unavailable
 *   5. NoHandlerFoundException     -> 404 Not Found
 *   6. Exception (catch-all)       -> 500 Internal Server Error
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles access denied exceptions (403 Forbidden).
     * Thrown when an authenticated user tries to access a resource
     * they don't have permission for (e.g., a PATIENT trying to access ADMIN endpoints).
     *
     * @param ex the AccessDeniedException
     * @return 403 response with error details
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .message("Access denied. You don't have permission to access this resource.")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handles authentication exceptions (401 Unauthorized).
     * Thrown when a user fails to authenticate (invalid or missing JWT token).
     *
     * @param ex the AuthenticationException
     * @return 401 response with error details
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message("Authentication failed. Please provide a valid token.")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles bad credentials exceptions (401 Unauthorized).
     * Thrown when login credentials are invalid.
     *
     * @param ex the BadCredentialsException
     * @return 401 response with error details
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message("Invalid credentials. Please check your email and password.")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles resource access exceptions (503 Service Unavailable).
     * Thrown when a downstream microservice is unreachable (connection refused, timeout).
     *
     * @param ex the ResourceAccessException
     * @return 503 response with error details
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccessException(ResourceAccessException ex) {
        log.error("Downstream service unavailable: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message("The requested service is temporarily unavailable. Please try again later.")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    /**
     * Handles no handler found exceptions (404 Not Found).
     * Thrown when no controller method matches the requested URL.
     *
     * @param ex the NoHandlerFoundException
     * @return 404 response with error details
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.warn("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message("The requested endpoint was not found: " + ex.getRequestURL())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Catch-all handler for any unhandled exceptions (500 Internal Server Error).
     * This is the last line of defense to ensure clients never receive raw stack traces.
     *
     * @param ex the unhandled Exception
     * @return 500 response with a generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception in API Gateway: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("An unexpected error occurred. Please try again later.")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
