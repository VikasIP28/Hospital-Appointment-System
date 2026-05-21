package com.hospital.appointment.dto;

import com.hospital.appointment.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for returning appointment data to clients.
 *
 * Includes all appointment entity fields plus the doctor's name
 * resolved from the Doctor Service via Feign client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    /** Unique appointment identifier */
    private String id;

    /** Patient's full name */
    private String patientName;

    /** Patient's email address */
    private String patientEmail;

    /** Referenced doctor's unique ID */
    private String doctorId;

    /** Doctor's name resolved from Doctor Service (may be "Unknown" on fallback) */
    private String doctorName;

    /** Scheduled appointment date and time */
    private LocalDateTime appointmentDate;

    /** Patient's reported symptoms or visit reason */
    private String symptoms;

    /** Current lifecycle status of the appointment */
    private AppointmentStatus status;

    /** Timestamp when the appointment was originally created */
    private LocalDateTime createdAt;
}
