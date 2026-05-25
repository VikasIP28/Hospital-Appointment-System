package com.hospital.logging.exception;

import com.hospital.logging.util.MdcUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error occurred while processing request: {} {}", request.getMethod(), request.getRequestURI(), ex);
        
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Validation Failed")
                .timestamp(LocalDateTime.now())
                .correlationId(MdcUtil.getCorrelationId())
                .errorType("VALIDATION_ERROR")
                .validationErrors(errors)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        log.warn("Business exception occurred: {} {}", request.getMethod(), request.getRequestURI(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(ex.getStatusCode().value())
                .message(ex.getReason())
                .timestamp(LocalDateTime.now())
                .correlationId(MdcUtil.getCorrelationId())
                .errorType("BUSINESS_ERROR")
                .build();

        return new ResponseEntity<>(errorResponse, ex.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex, HttpServletRequest request) {
        log.error("Unknown system error occurred: {} {}", request.getMethod(), request.getRequestURI(), ex);
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        org.springframework.web.bind.annotation.ResponseStatus responseStatus = 
            org.springframework.core.annotation.AnnotationUtils.findAnnotation(ex.getClass(), org.springframework.web.bind.annotation.ResponseStatus.class);
        
        if (responseStatus != null) {
            status = responseStatus.value();
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .message(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred.")
                .timestamp(LocalDateTime.now())
                .correlationId(MdcUtil.getCorrelationId())
                .errorType(status.is5xxServerError() ? "SYSTEM_ERROR" : "BUSINESS_ERROR")
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }
}
