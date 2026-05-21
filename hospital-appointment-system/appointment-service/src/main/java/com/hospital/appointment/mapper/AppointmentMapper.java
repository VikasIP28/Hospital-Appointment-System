package com.hospital.appointment.mapper;

import com.hospital.appointment.dto.AppointmentRequest;
import com.hospital.appointment.dto.AppointmentResponse;
import com.hospital.appointment.entity.Appointment;
import com.hospital.appointment.enums.AppointmentStatus;
import com.hospital.appointment.kafka.event.AppointmentEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Static utility class for mapping between Appointment domain objects.
 *
 * Provides conversion methods for:
 * - AppointmentRequest -> Appointment entity (for creation)
 * - Appointment entity -> AppointmentResponse DTO (for API responses)
 * - Appointment entity -> AppointmentEvent (for Kafka publishing)
 *
 * All methods are static since no instance state is needed.
 */
public final class AppointmentMapper {

    /** ISO-8601 formatter for date/time serialization in Kafka events */
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** Private constructor to prevent instantiation of utility class */
    private AppointmentMapper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Converts an AppointmentRequest DTO to an Appointment entity.
     * Sets the default status to PENDING and does NOT set the id
     * (MongoDB will auto-generate it).
     *
     * @param request the incoming appointment request
     * @return a new Appointment entity ready for persistence
     */
    public static Appointment toEntity(AppointmentRequest request) {
        return Appointment.builder()
                .patientName(request.getPatientName())
                .patientEmail(request.getPatientEmail())
                .doctorId(request.getDoctorId())
                .appointmentDate(request.getAppointmentDate())
                .symptoms(request.getSymptoms())
                .status(AppointmentStatus.PENDING)
                .build();
    }

    /**
     * Converts an Appointment entity to an AppointmentResponse DTO.
     * The doctorName field defaults to null; the caller should populate it
     * from the Doctor Service response when available.
     *
     * @param appointment the appointment entity from the database
     * @return an AppointmentResponse DTO for the API response
     */
    public static AppointmentResponse toResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .patientName(appointment.getPatientName())
                .patientEmail(appointment.getPatientEmail())
                .doctorId(appointment.getDoctorId())
                .appointmentDate(appointment.getAppointmentDate())
                .symptoms(appointment.getSymptoms())
                .status(appointment.getStatus())
                .createdAt(appointment.getCreatedAt())
                .build();
    }

    /**
     * Converts an Appointment entity to an AppointmentResponse DTO
     * with the doctor's name populated.
     *
     * @param appointment the appointment entity from the database
     * @param doctorName  the doctor's name resolved from the Doctor Service
     * @return an enriched AppointmentResponse DTO
     */
    public static AppointmentResponse toResponse(Appointment appointment, String doctorName) {
        AppointmentResponse response = toResponse(appointment);
        response.setDoctorName(doctorName);
        return response;
    }

    /**
     * Converts an Appointment entity to a Kafka AppointmentEvent.
     * Serializes date/time fields as ISO-8601 strings for cross-service
     * compatibility.
     *
     * @param appointment the appointment entity
     * @param eventType   the type of lifecycle event (e.g., "CREATED", "CONFIRMED")
     * @return an AppointmentEvent ready for Kafka publishing
     */
    public static AppointmentEvent toEvent(Appointment appointment, String eventType) {
        return AppointmentEvent.builder()
                .appointmentId(appointment.getId())
                .patientName(appointment.getPatientName())
                .patientEmail(appointment.getPatientEmail())
                .doctorId(appointment.getDoctorId())
                .appointmentDate(appointment.getAppointmentDate() != null
                        ? appointment.getAppointmentDate().format(ISO_FORMATTER)
                        : null)
                .symptoms(appointment.getSymptoms())
                .status(appointment.getStatus().name())
                .eventType(eventType)
                .timestamp(LocalDateTime.now().format(ISO_FORMATTER))
                .build();
    }
}
