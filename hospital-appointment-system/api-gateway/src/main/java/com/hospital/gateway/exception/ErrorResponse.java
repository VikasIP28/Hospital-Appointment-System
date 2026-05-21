package com.hospital.gateway.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Error Response DTO
 * ===================
 * A standardized error response object used across the API Gateway
 * to provide consistent error formatting for all error scenarios.
 *
 * This ensures clients always receive a predictable error structure
 * regardless of which downstream service caused the error or what
 * type of exception occurred.
 *
 * Example JSON response:
 * {
 *   "status": 503,
 *   "message": "Service is temporarily unavailable",
 *   "timestamp": "2026-05-20T18:25:00"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * HTTP status code (e.g., 400, 401, 403, 404, 500, 503)
     */
    private int status;

    /**
     * Human-readable error message describing what went wrong
     */
    private String message;

    /**
     * Timestamp when the error occurred, useful for debugging and log correlation
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
