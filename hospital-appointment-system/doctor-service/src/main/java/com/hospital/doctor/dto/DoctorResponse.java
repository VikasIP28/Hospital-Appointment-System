package com.hospital.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * Doctor Response DTO - Outgoing Data Transfer Object
 * ============================================================================
 * Used for returning doctor profile data to API consumers.
 * Contains all relevant doctor information in a clean, serializable format.
 * Uses @Builder pattern for convenient object construction in the service layer.
 * ============================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorResponse {

    /** Unique identifier of the doctor */
    private String id;

    /** Full name of the doctor */
    private String name;

    /** Medical specialization (e.g., CARDIOLOGY, NEUROLOGY) */
    private String specialization;

    /** Email address */
    private String email;

    /** Contact phone number */
    private String phone;

    /** Whether the doctor is currently available for appointments */
    private boolean availability;

    /** Timestamp when the doctor record was created */
    private LocalDateTime createdAt;
}
