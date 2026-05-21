package com.hospital.auth.enums;

/**
 * ============================================================================
 * Role Enumeration
 * ============================================================================
 * Defines the possible user roles within the Hospital Appointment System.
 * Each role determines the set of permissions and accessible endpoints
 * for the authenticated user.
 *
 * - ADMIN:   Full system access, user management, system configuration
 * - DOCTOR:  Manage appointments, view patient records, update availability
 * - PATIENT: Book appointments, view own records, manage profile
 * ============================================================================
 */
public enum Role {

    ADMIN,
    DOCTOR,
    PATIENT
}
