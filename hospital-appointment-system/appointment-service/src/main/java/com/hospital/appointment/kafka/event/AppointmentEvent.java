package com.hospital.appointment.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event payload representing an appointment lifecycle event.
 *
 * Published to various Kafka topics (appointment-created, appointment-confirmed,
 * appointment-rejected, appointment-reminder) to notify downstream consumers
 * such as the Notification Service.
 *
 * All date/time fields are serialized as ISO-8601 strings to ensure
 * cross-service compatibility regardless of date library.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentEvent {

    /** Unique appointment identifier */
    private String appointmentId;

    /** Patient's full name */
    private String patientName;

    /** Patient's email address (used by notification service for sending emails) */
    private String patientEmail;

    /** Referenced doctor's unique ID */
    private String doctorId;

    /** Scheduled appointment date/time as ISO-8601 string */
    private String appointmentDate;

    /** Patient's reported symptoms */
    private String symptoms;

    /** Current appointment status as string (e.g., "PENDING", "CONFIRMED") */
    private String status;

    /** Type of event that triggered this message (e.g., "CREATED", "CONFIRMED") */
    private String eventType;

    /** ISO-8601 timestamp of when this event was generated */
    private String timestamp;
}
