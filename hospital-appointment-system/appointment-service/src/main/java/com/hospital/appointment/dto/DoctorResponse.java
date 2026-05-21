package com.hospital.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO mirroring the response from the Doctor Service.
 *
 * Used by the Feign client to deserialize doctor information
 * when validating or enriching appointment data. The structure
 * must match the Doctor Service's DoctorResponse DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorResponse {

    /** Doctor's unique identifier */
    private String id;

    /** Doctor's full name */
    private String name;

    /** Doctor's medical specialization (e.g., Cardiology, Neurology) */
    private String specialization;

    /** Doctor's email address */
    private String email;

    /** Doctor's phone number */
    private String phone;

    /** Whether the doctor is currently available for appointments */
    private boolean availability;

    /** Timestamp when the doctor record was created */
    private LocalDateTime createdAt;
}
