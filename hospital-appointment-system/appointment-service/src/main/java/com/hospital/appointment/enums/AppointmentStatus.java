package com.hospital.appointment.enums;

/**
 * Enumeration representing the lifecycle states of an appointment.
 *
 * State transitions:
 *   PENDING -> CONFIRMED  (by doctor or admin)
 *   PENDING -> REJECTED   (by doctor or admin)
 *   PENDING -> CANCELLED  (by patient)
 *   CONFIRMED -> COMPLETED (by doctor or admin)
 *   CONFIRMED -> CANCELLED (by patient or admin)
 *
 * Terminal states: COMPLETED, CANCELLED, REJECTED
 */
public enum AppointmentStatus {

    /** Initial status when a patient books an appointment */
    PENDING,

    /** Appointment has been confirmed by the doctor or admin */
    CONFIRMED,

    /** Appointment has been rejected by the doctor or admin */
    REJECTED,

    /** Appointment has been completed successfully */
    COMPLETED,

    /** Appointment has been cancelled by the patient or admin */
    CANCELLED
}
