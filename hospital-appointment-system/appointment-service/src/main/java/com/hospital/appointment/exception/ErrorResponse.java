package com.hospital.appointment.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard error response DTO returned by the GlobalExceptionHandler.
 *
 * Provides a consistent error response format across all API endpoints
 * with HTTP status code, error message, and timestamp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /** HTTP status code (e.g., 400, 404, 500) */
    private int status;

    /** Human-readable error message */
    private String message;

    /** Timestamp when the error occurred */
    private LocalDateTime timestamp;
}
