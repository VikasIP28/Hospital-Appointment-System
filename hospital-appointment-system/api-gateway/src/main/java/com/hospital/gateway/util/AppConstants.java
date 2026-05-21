package com.hospital.gateway.util;

/**
 * Application Constants
 * ======================
 * Centralizes all constant values used across the API Gateway microservice.
 * Using constants instead of string literals prevents typos and ensures
 * consistency when referencing roles, headers, or route paths.
 *
 * Categories:
 *   - User Roles      : PATIENT, DOCTOR, ADMIN
 *   - HTTP Headers     : Authorization header key and Bearer prefix
 *   - Route Prefixes   : Gateway and downstream path prefixes
 *   - Error Messages   : Standardized error messages
 */
public final class AppConstants {

    // Private constructor to prevent instantiation of this utility class
    private AppConstants() {
        throw new UnsupportedOperationException("AppConstants is a utility class and cannot be instantiated");
    }

    // =========================================================================
    // User Roles
    // =========================================================================

    /** Patient role - can book/manage their own appointments */
    public static final String ROLE_PATIENT = "PATIENT";

    /** Doctor role - can manage availability and view assigned appointments */
    public static final String ROLE_DOCTOR = "DOCTOR";

    /** Admin role - has full system access and can manage all resources */
    public static final String ROLE_ADMIN = "ADMIN";

    // =========================================================================
    // HTTP Headers
    // =========================================================================

    /** The HTTP header key used to pass JWT tokens */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** The prefix for Bearer token authentication */
    public static final String BEARER_PREFIX = "Bearer ";

    /** The Content-Type header value for JSON payloads */
    public static final String CONTENT_TYPE_JSON = "application/json";

    // =========================================================================
    // Gateway Route Prefixes (incoming paths from clients)
    // =========================================================================

    /** Base path for all API requests through the gateway */
    public static final String API_BASE_PATH = "/api";

    /** Gateway prefix for auth service routes */
    public static final String GATEWAY_AUTH_PREFIX = "/api/auth";

    /** Gateway prefix for appointment service routes */
    public static final String GATEWAY_APPOINTMENT_PREFIX = "/api/appointments";

    /** Gateway prefix for doctor service routes */
    public static final String GATEWAY_DOCTOR_PREFIX = "/api/doctors";

    /** Gateway prefix for notification service routes */
    public static final String GATEWAY_NOTIFICATION_PREFIX = "/api/notifications";

    /** Gateway prefix for admin routes */
    public static final String GATEWAY_ADMIN_PREFIX = "/api/admin";

    // =========================================================================
    // Downstream Service Route Prefixes (paths on downstream services)
    // =========================================================================

    /** Downstream prefix for auth service */
    public static final String DOWNSTREAM_AUTH_PREFIX = "/auth";

    /** Downstream prefix for appointment service */
    public static final String DOWNSTREAM_APPOINTMENT_PREFIX = "/appointments";

    /** Downstream prefix for doctor service */
    public static final String DOWNSTREAM_DOCTOR_PREFIX = "/doctors";

    /** Downstream prefix for notification service */
    public static final String DOWNSTREAM_NOTIFICATION_PREFIX = "/notifications";

    // =========================================================================
    // Error Messages
    // =========================================================================

    /** Generic service unavailable error message */
    public static final String ERROR_SERVICE_UNAVAILABLE = "Service is temporarily unavailable. Please try again later.";

    /** Authentication failure error message */
    public static final String ERROR_AUTHENTICATION_FAILED = "Authentication failed. Please provide a valid token.";

    /** Access denied error message */
    public static final String ERROR_ACCESS_DENIED = "Access denied. You don't have permission to access this resource.";

    /** Generic unexpected error message */
    public static final String ERROR_UNEXPECTED = "An unexpected error occurred. Please try again later.";
}
