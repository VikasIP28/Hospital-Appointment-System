package com.hospital.notification.enums;

/**
 * Enum representing the different types of notifications that can be sent.
 * Each type corresponds to a specific appointment lifecycle event
 * consumed from the Kafka topics published by the appointment-service.
 */
public enum NotificationType {

    /** Sent when a new appointment is created by a patient */
    APPOINTMENT_CREATED,

    /** Sent when a doctor confirms/approves a pending appointment */
    APPOINTMENT_CONFIRMED,

    /** Sent when a doctor rejects a pending appointment */
    APPOINTMENT_REJECTED,

    /** Sent as a reminder before an upcoming confirmed appointment */
    APPOINTMENT_REMINDER,

    /** Sent when an appointment is cancelled by either patient or doctor */
    APPOINTMENT_CANCELLED
}
