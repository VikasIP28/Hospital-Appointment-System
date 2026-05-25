package com.hospital.appointment.exception;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an invalid appointment status transition is attempted.
 *
 * For example, trying to confirm an already REJECTED appointment, or
 * trying to cancel a COMPLETED appointment.
 *
 * Results in a 400 Bad Request HTTP response via the GlobalExceptionHandler.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidStatusTransitionException extends RuntimeException {

    /**
     * Constructs exception with a descriptive message.
     *
     * @param message the error message describing the invalid transition
     */
    public InvalidStatusTransitionException(String message) {
        super(message);
    }

    /**
     * Constructs exception with message and underlying cause.
     *
     * @param message the error message
     * @param cause   the original exception
     */
    public InvalidStatusTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
