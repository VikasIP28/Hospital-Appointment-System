package com.hospital.doctor.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * Global Exception Handler
 * ============================================================================
 * Centralized exception handling for all REST controllers in the doctor-service.
 * Uses @RestControllerAdvice to intercept exceptions thrown by controllers
 * and return standardized ErrorResponse objects.
 *
 * Handled exceptions:
 * - DoctorNotFoundException     → 404 Not Found
 * - MethodArgumentNotValidException → 400 Bad Request (validation failures)
 * - Exception (generic)         → 500 Internal Server Error
 * ============================================================================
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle DoctorNotFoundException - returns 404 Not Found.
     * Triggered when a requested doctor does not exist in the database.
     *
     * @param ex the DoctorNotFoundException
     * @return 404 response with error details
     */
    @ExceptionHandler(DoctorNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDoctorNotFoundException(DoctorNotFoundException ex) {
        log.error("Doctor not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle MethodArgumentNotValidException - returns 400 Bad Request.
     * Triggered when @Valid validation fails on request body fields
     * (e.g., @NotBlank, @Email constraints violated).
     *
     * Collects all field validation errors into a single comma-separated message.
     *
     * @param ex the MethodArgumentNotValidException containing field errors
     * @return 400 response with concatenated validation error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        // Collect all field-level validation error messages
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.error("Validation failed: {}", errorMessage);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle all other unhandled exceptions - returns 500 Internal Server Error.
     * Acts as a catch-all to prevent raw stack traces from being exposed to clients.
     *
     * @param ex the unhandled exception
     * @return 500 response with a generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("An unexpected error occurred: " + ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
