package com.hospital.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ============================================================================
 * Doctor Availability Response DTO
 * ============================================================================
 * Lightweight response specifically for availability checks.
 * Contains only the essential fields needed to determine whether
 * a doctor is available for appointments, along with identifying info.
 *
 * Used by the appointment-service to verify doctor availability
 * before scheduling an appointment.
 * ============================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAvailabilityResponse {

    /** Unique identifier of the doctor */
    private String doctorId;

    /** Name of the doctor */
    private String doctorName;

    /** Whether the doctor is currently available */
    private boolean available;

    /** Doctor's medical specialization */
    private String specialization;
}
