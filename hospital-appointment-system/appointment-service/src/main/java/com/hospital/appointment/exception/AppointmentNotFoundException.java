package com.hospital.appointment.exception;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an appointment is not found by its ID.
 *
 * Typically results in a 404 Not Found HTTP response via
 * the GlobalExceptionHandler.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class AppointmentNotFoundException extends RuntimeException {

    /**
     * Constructs exception with a descriptive message.
     *
     * @param message the error message
     */
    public AppointmentNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs exception with message and underlying cause.
     *
     * @param message the error message
     * @param cause   the original exception
     */
    public AppointmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
