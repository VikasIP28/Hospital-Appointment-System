package com.hospital.auth.service;

import com.hospital.auth.dto.AuthResponse;
import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.RegisterRequest;

import java.util.Map;

/**
 * ============================================================================
 * Auth Service Interface
 * ============================================================================
 * Defines the contract for authentication operations in the Hospital
 * Appointment System. This interface abstracts the authentication logic
 * to allow different implementations and easier testing.
 *
 * Operations:
 * - register: Create a new user account
 * - login: Authenticate a user and generate a JWT token
 * - validateToken: Verify a JWT token and extract user information
 * ============================================================================
 */
public interface AuthService {

    /**
     * Registers a new user account in the system.
     *
     * @param registerRequest the registration data (name, email, password, role)
     * @return an AuthResponse containing the JWT token and user details
     * @throws com.hospital.auth.exception.UserAlreadyExistsException if the email is already registered
     */
    AuthResponse register(RegisterRequest registerRequest);

    /**
     * Authenticates a user with email and password credentials.
     *
     * @param loginRequest the login credentials (email, password)
     * @return an AuthResponse containing the JWT token and user details
     * @throws com.hospital.auth.exception.AuthException if the credentials are invalid
     */
    AuthResponse login(LoginRequest loginRequest);

    /**
     * Validates a JWT token and extracts the user information.
     *
     * @param token the JWT token string to validate
     * @return a Map containing "valid" (boolean), "email" (String), and "role" (String)
     */
    Map<String, Object> validateToken(String token);
}
