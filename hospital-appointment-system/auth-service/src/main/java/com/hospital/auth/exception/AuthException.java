package com.hospital.auth.exception;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

/**
 * ============================================================================
 * Authentication Exception
 * ============================================================================
 * Custom exception thrown when authentication fails. This includes scenarios
 * such as:
 * - Invalid email/password combination during login
 * - Invalid or expired JWT token during validation
 * - Any other authentication-related failure
 *
 * This exception is handled by the GlobalExceptionHandler and results in
 * an HTTP 401 (Unauthorized) response.
 * ============================================================================
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthException extends RuntimeException {

    /**
     * Creates a new AuthException with the specified error message.
     *
     * @param message a description of the authentication failure
     */
    public AuthException(String message) {
        super(message);
    }

    /**
     * Creates a new AuthException with the specified error message and cause.
     *
     * @param message a description of the authentication failure
     * @param cause   the underlying exception that caused this error
     */
    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
