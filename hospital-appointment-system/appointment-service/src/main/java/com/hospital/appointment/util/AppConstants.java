package com.hospital.appointment.util;

/**
 * Application-wide constants for the Appointment Service.
 *
 * Centralizes all magic strings including Kafka topic names,
 * JWT-related constants, event type identifiers, and status
 * transition labels to avoid string duplication and typos.
 */
public final class AppConstants {

    /** Private constructor to prevent instantiation */
    private AppConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // ============================================================
    // Kafka Topic Names
    // ============================================================

    /** Topic for newly created appointment events */
    public static final String TOPIC_APPOINTMENT_CREATED = "appointment-created";

    /** Topic for confirmed appointment events */
    public static final String TOPIC_APPOINTMENT_CONFIRMED = "appointment-confirmed";

    /** Topic for rejected appointment events */
    public static final String TOPIC_APPOINTMENT_REJECTED = "appointment-rejected";

    /** Topic for appointment reminder events (upcoming appointments) */
    public static final String TOPIC_APPOINTMENT_REMINDER = "appointment-reminder";

    // ============================================================
    // Event Types (used in AppointmentEvent.eventType)
    // ============================================================

    /** Event type when an appointment is first created */
    public static final String EVENT_CREATED = "CREATED";

    /** Event type when an appointment is confirmed by doctor/admin */
    public static final String EVENT_CONFIRMED = "CONFIRMED";

    /** Event type when an appointment is rejected by doctor/admin */
    public static final String EVENT_REJECTED = "REJECTED";

    /** Event type when an appointment reminder is triggered */
    public static final String EVENT_REMINDER = "REMINDER";

    /** Event type when an appointment is cancelled */
    public static final String EVENT_CANCELLED = "CANCELLED";

    // ============================================================
    // JWT Constants
    // ============================================================

    /** Authorization header name */
    public static final String AUTH_HEADER = "Authorization";

    /** Bearer token prefix in Authorization header */
    public static final String BEARER_PREFIX = "Bearer ";

    /** JWT claim key for user role */
    public static final String JWT_ROLE_CLAIM = "role";

    /** JWT claim key for user email */
    public static final String JWT_EMAIL_CLAIM = "email";

    // ============================================================
    // Role Constants (as stored in JWT, with ROLE_ prefix for Spring Security)
    // ============================================================

    /** Patient role identifier */
    public static final String ROLE_PATIENT = "ROLE_PATIENT";

    /** Doctor role identifier */
    public static final String ROLE_DOCTOR = "ROLE_DOCTOR";

    /** Admin role identifier */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    // ============================================================
    // Resilience4j Instance Names
    // ============================================================

    /** Circuit breaker instance name for Doctor Service calls */
    public static final String CB_DOCTOR_SERVICE = "doctorService";

    // ============================================================
    // Scheduling Constants
    // ============================================================

    /** Reminder check interval: 1 hour in milliseconds */
    public static final long REMINDER_CHECK_INTERVAL_MS = 3600000L;

    /** Reminder window: 24 hours ahead */
    public static final int REMINDER_WINDOW_HOURS = 24;
}
