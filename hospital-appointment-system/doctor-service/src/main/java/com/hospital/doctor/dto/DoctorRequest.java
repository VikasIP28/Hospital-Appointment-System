package com.hospital.doctor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ============================================================================
 * Doctor Request DTO - Incoming Data Transfer Object
 * ============================================================================
 * Used for creating and updating doctor profiles. Contains Jakarta Bean
 * Validation annotations to ensure data integrity before processing.
 *
 * Required fields: name, specialization, email
 * Optional fields: phone, availability
 * ============================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorRequest {

    /** Doctor's full name - required, cannot be blank */
    @NotBlank(message = "Doctor name is required")
    private String name;

    /**
     * Medical specialization - required, cannot be blank.
     * Should match one of the Specialization enum values
     * (e.g., "CARDIOLOGY", "NEUROLOGY", "ORTHOPEDICS").
     */
    @NotBlank(message = "Specialization is required")
    private String specialization;

    /** Email address - required, must be a valid email format */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    /** Contact phone number - optional */
    private String phone;

    /** Availability status - defaults to true if not provided */
    @Builder.Default
    private boolean availability = true;
}
