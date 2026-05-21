package com.hospital.doctor.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * Error Response DTO
 * ============================================================================
 * Standardized error response format returned by the GlobalExceptionHandler.
 * Provides a consistent structure for all error responses from the API.
 *
 * Example JSON response:
 * {
 *   "status": 404,
 *   "message": "Doctor not found with id: abc123",
 *   "timestamp": "2026-05-20T18:30:00"
 * }
 * ============================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /** HTTP status code (e.g., 400, 404, 500) */
    private int status;

    /** Human-readable error message describing what went wrong */
    private String message;

    /** Timestamp when the error occurred */
    private LocalDateTime timestamp;
}
