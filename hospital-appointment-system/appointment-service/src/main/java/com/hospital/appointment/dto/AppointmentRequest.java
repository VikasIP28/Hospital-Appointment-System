package com.hospital.appointment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for creating a new appointment.
 *
 * Contains validated patient and scheduling information.
 * The doctorId references a doctor managed by the Doctor Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequest {

    /** Patient's full name - must not be blank */
    @NotBlank(message = "Patient name is required")
    private String patientName;

    /** Patient's email address - must be a valid email format */
    @NotBlank(message = "Patient email is required")
    @Email(message = "Patient email must be a valid email address")
    private String patientEmail;

    /** ID of the doctor to book with - must reference a valid doctor */
    @NotBlank(message = "Doctor ID is required")
    private String doctorId;

    /** Desired appointment date/time - must be in the future */
    @NotNull(message = "Appointment date is required")
    @Future(message = "Appointment date must be in the future")
    private LocalDateTime appointmentDate;

    /** Optional description of symptoms or reason for visit */
    private String symptoms;
}
