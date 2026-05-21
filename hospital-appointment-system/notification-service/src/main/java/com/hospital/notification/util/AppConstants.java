package com.hospital.notification.util;

/**
 * Application-wide constants for the notification-service.
 *
 * Centralizes Kafka topic names and notification message templates
 * to ensure consistency across consumers and services.
 */
public final class AppConstants {

    private AppConstants() {
        // Utility class - prevent instantiation
    }

    // ==========================================
    // Kafka Topic Names
    // ==========================================
    /** Topic for newly created appointment events */
    public static final String TOPIC_APPOINTMENT_CREATED = "appointment-created";

    /** Topic for confirmed/approved appointment events */
    public static final String TOPIC_APPOINTMENT_CONFIRMED = "appointment-confirmed";

    /** Topic for rejected appointment events */
    public static final String TOPIC_APPOINTMENT_REJECTED = "appointment-rejected";

    /** Topic for appointment reminder events */
    public static final String TOPIC_APPOINTMENT_REMINDER = "appointment-reminder";

    /** Topic for cancelled appointment events */
    public static final String TOPIC_APPOINTMENT_CANCELLED = "appointment-cancelled";

    /** Consumer group ID for the notification service */
    public static final String KAFKA_GROUP_ID = "notification-group";

    // ==========================================
    // Notification Message Templates
    // ==========================================

    /**
     * Template for appointment creation notifications.
     * Placeholders: {patientName}, {appointmentDate}, {appointmentId}
     */
    public static final String MSG_APPOINTMENT_CREATED =
            "Dear %s, your appointment has been successfully created for %s. " +
            "Your appointment ID is: %s. Please wait for doctor confirmation.";

    /**
     * Template for appointment confirmation notifications.
     * Placeholders: {patientName}, {appointmentDate}, {appointmentId}
     */
    public static final String MSG_APPOINTMENT_CONFIRMED =
            "Dear %s, great news! Your appointment (ID: %s) scheduled for %s " +
            "has been confirmed by your doctor. Please arrive 15 minutes early.";

    /**
     * Template for appointment rejection notifications.
     * Placeholders: {patientName}, {appointmentId}
     */
    public static final String MSG_APPOINTMENT_REJECTED =
            "Dear %s, we regret to inform you that your appointment (ID: %s) " +
            "has been rejected by the doctor. Please book a new appointment with " +
            "another available time slot or doctor.";

    /**
     * Template for appointment reminder notifications.
     * Placeholders: {patientName}, {appointmentDate}, {appointmentId}
     */
    public static final String MSG_APPOINTMENT_REMINDER =
            "Dear %s, this is a reminder for your upcoming appointment (ID: %s) " +
            "scheduled for %s. Please ensure you arrive on time.";

    /**
     * Template for appointment cancellation notifications.
     * Placeholders: {patientName}, {appointmentId}
     */
    public static final String MSG_APPOINTMENT_CANCELLED =
            "Dear %s, your appointment (ID: %s) has been cancelled. " +
            "If you did not request this cancellation, please contact the hospital.";

    // ==========================================
    // Email Subject Lines
    // ==========================================
    public static final String SUBJECT_APPOINTMENT_CREATED = "Appointment Created - Hospital Appointment System";
    public static final String SUBJECT_APPOINTMENT_CONFIRMED = "Appointment Confirmed - Hospital Appointment System";
    public static final String SUBJECT_APPOINTMENT_REJECTED = "Appointment Rejected - Hospital Appointment System";
    public static final String SUBJECT_APPOINTMENT_REMINDER = "Appointment Reminder - Hospital Appointment System";
    public static final String SUBJECT_APPOINTMENT_CANCELLED = "Appointment Cancelled - Hospital Appointment System";
}
