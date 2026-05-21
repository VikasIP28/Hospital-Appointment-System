package com.hospital.appointment.repository;

import com.hospital.appointment.entity.Appointment;
import com.hospital.appointment.enums.AppointmentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB repository interface for Appointment documents.
 *
 * Provides CRUD operations plus custom query methods for filtering
 * appointments by doctor, patient, status, and date ranges.
 */
@Repository
public interface AppointmentRepository extends MongoRepository<Appointment, String> {

    /**
     * Find all appointments for a specific doctor.
     *
     * @param doctorId the doctor's unique identifier
     * @return list of appointments assigned to the doctor
     */
    List<Appointment> findByDoctorId(String doctorId);

    /**
     * Find appointments for a doctor filtered by status.
     * Useful for getting pending appointments that need doctor confirmation.
     *
     * @param doctorId the doctor's unique identifier
     * @param status   the appointment status to filter by
     * @return list of matching appointments
     */
    List<Appointment> findByDoctorIdAndStatus(String doctorId, AppointmentStatus status);

    /**
     * Find all appointments for a specific patient by their email.
     *
     * @param patientEmail the patient's email address
     * @return list of the patient's appointments
     */
    List<Appointment> findByPatientEmail(String patientEmail);

    /**
     * Find all appointments with a given status.
     *
     * @param status the status to filter by
     * @return list of appointments matching the status
     */
    List<Appointment> findByStatus(AppointmentStatus status);

    /**
     * Count the number of appointments with a specific status.
     * Used for analytics and dashboard statistics.
     *
     * @param status the status to count
     * @return count of appointments with the given status
     */
    long countByStatus(AppointmentStatus status);

    /**
     * Count total appointments assigned to a specific doctor.
     *
     * @param doctorId the doctor's unique identifier
     * @return count of appointments for the doctor
     */
    long countByDoctorId(String doctorId);

    /**
     * Find confirmed appointments within a date range.
     * Used by the reminder scheduler to find upcoming appointments.
     *
     * @param status the appointment status (typically CONFIRMED)
     * @param start  the start of the date range
     * @param end    the end of the date range
     * @return list of confirmed appointments in the date range
     */
    List<Appointment> findByStatusAndAppointmentDateBetween(
            AppointmentStatus status, LocalDateTime start, LocalDateTime end);
}
