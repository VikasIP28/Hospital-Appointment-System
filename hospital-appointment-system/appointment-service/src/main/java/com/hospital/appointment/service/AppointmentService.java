package com.hospital.appointment.service;

import com.hospital.appointment.dto.AppointmentRequest;
import com.hospital.appointment.dto.AppointmentResponse;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * Appointment Service Interface
 * ============================================================================
 * Defines the business operations for managing the appointment lifecycle.
 *
 * Supported operations:
 * - CRUD for appointments
 * - Status transitions: confirm, reject, cancel
 * - Doctor-specific queries (pending appointments)
 * - Aggregated statistics for admin analytics
 *
 * The implementation integrates with:
 * - Doctor Service (via Feign + Resilience4j)
 * - Kafka (event publishing on status changes)
 * - MongoDB (persistence)
 * ============================================================================
 */
public interface AppointmentService {

    /**
     * Creates a new appointment in PENDING status.
     * Validates the doctor exists via Doctor Service (with circuit breaker),
     * persists the appointment, and publishes an appointment-created Kafka event.
     *
     * @param request the appointment creation request
     * @return the created appointment response with doctor name populated
     */
    AppointmentResponse createAppointment(AppointmentRequest request);

    /**
     * Retrieves all appointments in the system.
     *
     * @return list of all appointment responses
     */
    List<AppointmentResponse> getAllAppointments();

    /**
     * Retrieves a single appointment by its unique ID.
     *
     * @param id the appointment ID
     * @return the appointment response
     * @throws com.hospital.appointment.exception.AppointmentNotFoundException if not found
     */
    AppointmentResponse getAppointmentById(String id);

    /**
     * Confirms a PENDING appointment (transition: PENDING → CONFIRMED).
     * Publishes an appointment-confirmed Kafka event upon success.
     *
     * @param id the appointment ID to confirm
     * @return the updated appointment response
     * @throws com.hospital.appointment.exception.InvalidStatusTransitionException if current status is not PENDING
     */
    AppointmentResponse confirmAppointment(String id);

    /**
     * Rejects a PENDING appointment (transition: PENDING → REJECTED).
     * Publishes an appointment-rejected Kafka event upon success.
     *
     * @param id the appointment ID to reject
     * @return the updated appointment response
     * @throws com.hospital.appointment.exception.InvalidStatusTransitionException if current status is not PENDING
     */
    AppointmentResponse rejectAppointment(String id);

    /**
     * Cancels an appointment (transition: PENDING/CONFIRMED → CANCELLED).
     * Appointments that are already COMPLETED, CANCELLED, or REJECTED cannot be cancelled.
     *
     * @param id the appointment ID to cancel
     * @return the updated appointment response
     * @throws com.hospital.appointment.exception.InvalidStatusTransitionException if status cannot transition
     */
    AppointmentResponse cancelAppointment(String id);

    /**
     * Retrieves all PENDING appointments for a specific doctor.
     * Used by doctors to view their incoming appointment requests.
     *
     * @param doctorId the doctor's unique ID
     * @return list of pending appointments for the specified doctor
     */
    List<AppointmentResponse> getPendingAppointmentsByDoctorId(String doctorId);

    /**
     * Generates aggregated appointment statistics for admin dashboards.
     *
     * @return map containing totalAppointments, counts by status, etc.
     */
    Map<String, Object> getAppointmentStatistics();
}
