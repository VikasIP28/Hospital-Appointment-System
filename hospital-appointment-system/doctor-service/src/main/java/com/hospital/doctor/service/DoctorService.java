package com.hospital.doctor.service;

import com.hospital.doctor.dto.DoctorAvailabilityResponse;
import com.hospital.doctor.dto.DoctorRequest;
import com.hospital.doctor.dto.DoctorResponse;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * Doctor Service Interface
 * ============================================================================
 * Defines the business operations available for doctor management.
 * Implemented by DoctorServiceImpl which contains the actual business logic.
 *
 * This interface follows the Interface Segregation Principle (ISP) and
 * allows for easy mocking in unit tests.
 * ============================================================================
 */
public interface DoctorService {

    /**
     * Create a new doctor profile.
     *
     * @param doctorRequest the doctor registration data
     * @return the created doctor's response DTO
     */
    DoctorResponse createDoctor(DoctorRequest doctorRequest);

    /**
     * Retrieve all doctors in the system.
     *
     * @return list of all doctor response DTOs
     */
    List<DoctorResponse> getAllDoctors();

    /**
     * Retrieve a specific doctor by their unique ID.
     *
     * @param id the doctor's unique identifier
     * @return the doctor's response DTO
     * @throws com.hospital.doctor.exception.DoctorNotFoundException if not found
     */
    DoctorResponse getDoctorById(String id);

    /**
     * Check the availability status of a specific doctor.
     *
     * @param id the doctor's unique identifier
     * @return availability response with doctor info and status
     * @throws com.hospital.doctor.exception.DoctorNotFoundException if not found
     */
    DoctorAvailabilityResponse checkAvailability(String id);

    /**
     * Update a doctor's availability status.
     *
     * @param id        the doctor's unique identifier
     * @param available the new availability status
     * @return the updated doctor's response DTO
     * @throws com.hospital.doctor.exception.DoctorNotFoundException if not found
     */
    DoctorResponse updateAvailability(String id, boolean available);

    /**
     * Find all doctors with a specific medical specialization.
     *
     * @param specialization the medical specialization to filter by
     * @return list of doctors with the specified specialization
     */
    List<DoctorResponse> getDoctorsBySpecialization(String specialization);

    /**
     * Generate workload analytics data for administrators.
     * Provides aggregate statistics about doctor distribution and availability.
     *
     * @return map containing analytics metrics (totalDoctors, availableDoctors, etc.)
     */
    Map<String, Object> getDoctorWorkloadAnalytics();
}
