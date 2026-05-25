package com.hospital.notification.exception;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a notification cannot be found by its ID.
 * Typically occurs when trying to resend a notification that doesn't exist.
 * Handled by GlobalExceptionHandler to return a 404 Not Found response.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(String message) {
        super(message);
    }

    public NotificationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
