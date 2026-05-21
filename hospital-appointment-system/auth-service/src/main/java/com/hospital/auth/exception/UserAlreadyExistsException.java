package com.hospital.auth.exception;

/**
 * ============================================================================
 * User Already Exists Exception
 * ============================================================================
 * Custom exception thrown during user registration when an account with
 * the same email address already exists in the database.
 *
 * This exception is handled by the GlobalExceptionHandler and results in
 * an HTTP 409 (Conflict) response.
 * ============================================================================
 */
public class UserAlreadyExistsException extends RuntimeException {

    /**
     * Creates a new UserAlreadyExistsException with the specified error message.
     *
     * @param message a description indicating which email already exists
     */
    public UserAlreadyExistsException(String message) {
        super(message);
    }

    /**
     * Creates a new UserAlreadyExistsException with the specified error message and cause.
     *
     * @param message a description indicating which email already exists
     * @param cause   the underlying exception that caused this error
     */
    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
