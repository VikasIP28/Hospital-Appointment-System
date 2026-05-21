package com.hospital.notification.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standardized error response DTO returned by the GlobalExceptionHandler.
 * Provides consistent error response format across all API endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /** HTTP status code (e.g., 404, 500) */
    private int status;

    /** Human-readable error message */
    private String message;

    /** Timestamp of when the error occurred */
    private LocalDateTime timestamp;

    /** The API path that was requested when the error occurred */
    private String path;
}
