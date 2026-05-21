package com.hospital.auth.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * Error Response DTO
 * ============================================================================
 * Standardized error response structure returned by the GlobalExceptionHandler
 * for all error scenarios. Provides a consistent JSON format for clients to
 * parse and display error information.
 *
 * Example JSON response:
 * {
 *     "status": 401,
 *     "message": "Invalid email or password",
 *     "timestamp": "2026-05-20T18:30:00"
 * }
 * ============================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * The HTTP status code (e.g., 400, 401, 409, 500).
     */
    private int status;

    /**
     * A human-readable description of the error.
     */
    private String message;

    /**
     * The timestamp when the error occurred, for debugging and audit purposes.
     */
    private LocalDateTime timestamp;
}
