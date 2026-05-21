package com.hospital.auth.util;

/**
 * ============================================================================
 * Application Constants
 * ============================================================================
 * Centralized constants used throughout the Auth Service to avoid
 * hardcoded strings and ensure consistency.
 * ============================================================================
 */
public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    // --- JWT Constants ---
    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";

    // --- Role Constants ---
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_DOCTOR = "DOCTOR";
    public static final String ROLE_PATIENT = "PATIENT";
    public static final String ROLE_PREFIX = "ROLE_";

    // --- Response Messages ---
    public static final String USER_REGISTERED_SUCCESS = "User registered successfully";
    public static final String LOGIN_SUCCESS = "Login successful";
    public static final String INVALID_CREDENTIALS = "Invalid email or password";
    public static final String USER_ALREADY_EXISTS = "User with email '%s' already exists";
    public static final String USER_NOT_FOUND = "User not found with email: %s";
    public static final String TOKEN_VALID = "Token is valid";
    public static final String TOKEN_INVALID = "Token is invalid or expired";
}
