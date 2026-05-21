package com.hospital.appointment.entity;

import com.hospital.appointment.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB document representing an appointment in the hospital system.
 *
 * Each appointment links a patient (by name/email) to a doctor (by doctorId)
 * at a specific date/time. The status field tracks the appointment lifecycle
 * from PENDING through to COMPLETED or CANCELLED.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "appointments")
public class Appointment {

    /** Unique MongoDB-generated identifier */
    @Id
    private String id;

    /** Full name of the patient */
    private String patientName;

    /** Email address of the patient (used for notifications) */
    private String patientEmail;

    /** Reference to the doctor in the Doctor Service */
    private String doctorId;

    /** Scheduled date and time of the appointment */
    private LocalDateTime appointmentDate;

    /** Patient-reported symptoms or reason for the visit */
    private String symptoms;

    /** Current lifecycle status of the appointment, defaults to PENDING */
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PENDING;

    /** Timestamp when the appointment record was created */
    @CreatedDate
    private LocalDateTime createdAt;
}
