package com.hospital.doctor.exception;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

/**
 * ============================================================================
 * Doctor Not Found Exception
 * ============================================================================
 * Custom exception thrown when a doctor with the specified ID or criteria
 * cannot be found in the database. This exception is caught by the
 * GlobalExceptionHandler and mapped to an HTTP 404 (Not Found) response.
 *
 * Usage examples:
 * - getDoctorById() when the doctor ID doesn't exist
 * - checkAvailability() when the specified doctor is not found
 * ============================================================================
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class DoctorNotFoundException extends RuntimeException {

    /**
     * Create a new DoctorNotFoundException with a descriptive message.
     *
     * @param message detailed description of which doctor was not found
     */
    public DoctorNotFoundException(String message) {
        super(message);
    }

    /**
     * Create a new DoctorNotFoundException with a message and root cause.
     *
     * @param message detailed description of which doctor was not found
     * @param cause   the underlying exception that caused this
     */
    public DoctorNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
