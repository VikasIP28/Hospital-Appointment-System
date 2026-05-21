package com.hospital.notification.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event DTO representing an appointment lifecycle event.
 *
 * This class MUST exactly match the AppointmentEvent published by the appointment-service.
 * It is deserialized from JSON by the Kafka JsonDeserializer when events arrive
 * on appointment-related topics.
 *
 * Fields:
 * - appointmentId: unique identifier of the appointment
 * - patientName: display name of the patient (used in notification messages)
 * - patientEmail: email address to send the notification to
 * - doctorId: the doctor involved in the appointment
 * - appointmentDate: ISO date string of the appointment
 * - symptoms: patient-reported symptoms
 * - status: current status of the appointment (PENDING, CONFIRMED, REJECTED, CANCELLED)
 * - eventType: the type of event (CREATED, CONFIRMED, REJECTED, CANCELLED, REMINDER)
 * - timestamp: ISO timestamp of when the event was published
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentEvent {

    /** Unique identifier of the appointment that triggered this event */
    private String appointmentId;

    /** Full name of the patient (used for personalized notification messages) */
    private String patientName;

    /** Patient's email address (notification recipient) */
    private String patientEmail;

    /** Identifier of the doctor assigned to this appointment */
    private String doctorId;

    /** The appointment date as an ISO date string (e.g., "2026-06-15") */
    private String appointmentDate;

    /** Symptoms reported by the patient */
    private String symptoms;

    /** Current appointment status: PENDING, CONFIRMED, REJECTED, CANCELLED */
    private String status;

    /** The type of lifecycle event: CREATED, CONFIRMED, REJECTED, CANCELLED, REMINDER */
    private String eventType;

    /** ISO timestamp of when this event was published by the appointment-service */
    private String timestamp;
}
