package com.hospital.doctor.util;

/**
 * ============================================================================
 * Application Constants
 * ============================================================================
 * Centralized constants used throughout the doctor-service microservice.
 * Storing constants in a single class promotes:
 * - DRY principle: no magic strings/numbers scattered across the codebase
 * - Easy maintenance: change a value in one place
 * - Self-documenting code: named constants explain their purpose
 *
 * This is a utility class and should not be instantiated.
 * ============================================================================
 */
public final class AppConstants {

    /** Private constructor to prevent instantiation of this utility class */
    private AppConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ========================================================================
    // Service Information
    // ========================================================================

    /** Name of this microservice */
    public static final String SERVICE_NAME = "doctor-service";

    /** API version prefix (for future versioning) */
    public static final String API_VERSION = "v1";

    // ========================================================================
    // API Paths
    // ========================================================================

    /** Base path for doctor endpoints */
    public static final String DOCTORS_BASE_PATH = "/doctors";

    /** Base path for admin endpoints */
    public static final String ADMIN_BASE_PATH = "/admin";

    /** Path for actuator endpoints */
    public static final String ACTUATOR_PATH = "/actuator/**";

    /** Path for simulation endpoints */
    public static final String SIMULATE_PATH = "/doctors/simulate/**";

    // ========================================================================
    // Security Constants
    // ========================================================================

    /** Authorization header name */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer token prefix in Authorization header */
    public static final String BEARER_PREFIX = "Bearer ";

    /** Length of the Bearer prefix string */
    public static final int BEARER_PREFIX_LENGTH = 7;

    /** Role claim key in JWT token */
    public static final String JWT_ROLE_CLAIM = "role";

    /** Spring Security role prefix */
    public static final String ROLE_PREFIX = "ROLE_";

    // ========================================================================
    // Role Names
    // ========================================================================

    /** Admin role identifier */
    public static final String ROLE_ADMIN = "ADMIN";

    /** Doctor role identifier */
    public static final String ROLE_DOCTOR = "DOCTOR";

    /** Patient role identifier */
    public static final String ROLE_PATIENT = "PATIENT";

    // ========================================================================
    // Simulation Constants
    // ========================================================================

    /** Delay duration for slow response simulation in milliseconds */
    public static final long SLOW_RESPONSE_DELAY_MS = 5000L;

    /** Probability threshold for random failure simulation (0.5 = 50%) */
    public static final double RANDOM_FAILURE_PROBABILITY = 0.5;

    // ========================================================================
    // Error Messages
    // ========================================================================

    /** Error message template for doctor not found by ID */
    public static final String DOCTOR_NOT_FOUND_BY_ID = "Doctor not found with id: ";

    /** Error message for simulated failure */
    public static final String SIMULATED_FAILURE_MESSAGE = "Simulated service failure for testing resilience patterns";

    /** Error message for random failure */
    public static final String RANDOM_FAILURE_MESSAGE = "Random failure occurred! (50% chance simulation)";

    // ========================================================================
    // Analytics Keys
    // ========================================================================

    /** Key for total doctors count in analytics response */
    public static final String ANALYTICS_TOTAL_DOCTORS = "totalDoctors";

    /** Key for available doctors count in analytics response */
    public static final String ANALYTICS_AVAILABLE_DOCTORS = "availableDoctors";

    /** Key for unavailable doctors count in analytics response */
    public static final String ANALYTICS_UNAVAILABLE_DOCTORS = "unavailableDoctors";

    /** Key for doctors by specialization breakdown in analytics response */
    public static final String ANALYTICS_BY_SPECIALIZATION = "doctorsBySpecialization";
}
