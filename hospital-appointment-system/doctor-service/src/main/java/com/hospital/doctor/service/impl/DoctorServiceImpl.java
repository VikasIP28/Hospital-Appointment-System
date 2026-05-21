package com.hospital.doctor.service.impl;

import com.hospital.doctor.dto.DoctorAvailabilityResponse;
import com.hospital.doctor.dto.DoctorRequest;
import com.hospital.doctor.dto.DoctorResponse;
import com.hospital.doctor.entity.Doctor;
import com.hospital.doctor.exception.DoctorNotFoundException;
import com.hospital.doctor.repository.DoctorRepository;
import com.hospital.doctor.service.DoctorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * Doctor Service Implementation
 * ============================================================================
 * Contains all the business logic for doctor management operations.
 * Handles DTO-to-entity mapping, database interactions via DoctorRepository,
 * and analytics aggregation.
 *
 * This class follows these patterns:
 * - Service layer pattern: encapsulates business logic between controllers and repository
 * - DTO pattern: never exposes raw entities to API consumers
 * - SLF4J logging: provides detailed operation logging for debugging and monitoring
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorServiceImpl implements DoctorService {

    /** Repository for MongoDB CRUD operations on Doctor documents */
    private final DoctorRepository doctorRepository;

    /**
     * Create a new doctor profile from the provided request data.
     * Maps the DoctorRequest DTO to a Doctor entity, sets the creation timestamp,
     * saves to MongoDB, and returns the saved doctor as a DoctorResponse.
     *
     * @param doctorRequest the incoming doctor data
     * @return DoctorResponse containing the saved doctor's information
     */
    @Override
    public DoctorResponse createDoctor(DoctorRequest doctorRequest) {
        log.info("Creating new doctor with name: {} and specialization: {}",
                doctorRequest.getName(), doctorRequest.getSpecialization());

        // Map DTO to entity
        Doctor doctor = Doctor.builder()
                .name(doctorRequest.getName())
                .specialization(doctorRequest.getSpecialization())
                .email(doctorRequest.getEmail())
                .phone(doctorRequest.getPhone())
                .availability(doctorRequest.isAvailability())
                .createdAt(LocalDateTime.now()) // Set creation timestamp explicitly
                .build();

        // Persist to MongoDB
        Doctor savedDoctor = doctorRepository.save(doctor);
        log.info("Doctor created successfully with id: {}", savedDoctor.getId());

        // Map entity back to response DTO
        return mapToResponse(savedDoctor);
    }

    /**
     * Retrieve all doctors from the database.
     * Maps each Doctor entity to a DoctorResponse DTO.
     *
     * @return list of all doctor profiles as response DTOs
     */
    @Override
    public List<DoctorResponse> getAllDoctors() {
        log.info("Fetching all doctors");

        List<Doctor> doctors = doctorRepository.findAll();
        log.info("Found {} doctors in the database", doctors.size());

        return doctors.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve a specific doctor by their unique ID.
     * Throws DoctorNotFoundException if no doctor is found.
     *
     * @param id the doctor's MongoDB ObjectId
     * @return DoctorResponse for the found doctor
     * @throws DoctorNotFoundException if no doctor exists with the given ID
     */
    @Override
    public DoctorResponse getDoctorById(String id) {
        log.info("Fetching doctor with id: {}", id);

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor not found with id: " + id));

        log.info("Found doctor: {} ({})", doctor.getName(), doctor.getSpecialization());
        return mapToResponse(doctor);
    }

    /**
     * Check whether a specific doctor is available for appointments.
     * Returns a lightweight DoctorAvailabilityResponse with only the
     * fields needed for availability checks.
     *
     * @param id the doctor's unique identifier
     * @return availability status and basic doctor info
     * @throws DoctorNotFoundException if no doctor exists with the given ID
     */
    @Override
    public DoctorAvailabilityResponse checkAvailability(String id) {
        log.info("Checking availability for doctor with id: {}", id);

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor not found with id: " + id));

        log.info("Doctor {} availability: {}", doctor.getName(), doctor.isAvailability());

        return DoctorAvailabilityResponse.builder()
                .doctorId(doctor.getId())
                .doctorName(doctor.getName())
                .available(doctor.isAvailability())
                .specialization(doctor.getSpecialization())
                .build();
    }

    /**
     * Update a doctor's availability status (available/unavailable).
     * This is used to mark doctors as available or unavailable for appointments.
     *
     * @param id        the doctor's unique identifier
     * @param available the new availability status
     * @return DoctorResponse with the updated availability
     * @throws DoctorNotFoundException if no doctor exists with the given ID
     */
    @Override
    public DoctorResponse updateAvailability(String id, boolean available) {
        log.info("Updating availability for doctor id: {} to: {}", id, available);

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor not found with id: " + id));

        // Update the availability field
        doctor.setAvailability(available);
        Doctor updatedDoctor = doctorRepository.save(doctor);

        log.info("Doctor {} availability updated to: {}", updatedDoctor.getName(), available);
        return mapToResponse(updatedDoctor);
    }

    /**
     * Find all doctors with a specific medical specialization.
     * Useful for patients looking for doctors in a specific field.
     *
     * @param specialization the specialization to filter by (e.g., "CARDIOLOGY")
     * @return list of doctors with the matching specialization
     */
    @Override
    public List<DoctorResponse> getDoctorsBySpecialization(String specialization) {
        log.info("Fetching doctors with specialization: {}", specialization);

        List<Doctor> doctors = doctorRepository.findBySpecialization(specialization);
        log.info("Found {} doctors with specialization: {}", doctors.size(), specialization);

        return doctors.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Generate comprehensive workload analytics for administrators.
     * Provides aggregate statistics about the doctor workforce including:
     * - Total number of registered doctors
     * - Number of currently available doctors
     * - Number of currently unavailable doctors
     * - Breakdown of doctor counts by specialization
     *
     * @return map containing analytics metrics
     */
    @Override
    public Map<String, Object> getDoctorWorkloadAnalytics() {
        log.info("Generating doctor workload analytics");

        List<Doctor> allDoctors = doctorRepository.findAll();
        List<Doctor> availableDoctors = doctorRepository.findByAvailability(true);
        List<Doctor> unavailableDoctors = doctorRepository.findByAvailability(false);

        // Calculate doctor count by specialization
        // Groups doctors by their specialization field and counts each group
        Map<String, Long> doctorsBySpecialization = allDoctors.stream()
                .collect(Collectors.groupingBy(
                        Doctor::getSpecialization,
                        Collectors.counting()
                ));

        // Build the analytics response map
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalDoctors", allDoctors.size());
        analytics.put("availableDoctors", availableDoctors.size());
        analytics.put("unavailableDoctors", unavailableDoctors.size());
        analytics.put("doctorsBySpecialization", doctorsBySpecialization);

        log.info("Analytics generated - Total: {}, Available: {}, Unavailable: {}",
                allDoctors.size(), availableDoctors.size(), unavailableDoctors.size());

        return analytics;
    }

    /**
     * Map a Doctor entity to a DoctorResponse DTO.
     * This private helper method ensures consistent mapping across all operations.
     * Keeps entity details (MongoDB annotations, etc.) hidden from API consumers.
     *
     * @param doctor the Doctor entity to map
     * @return DoctorResponse DTO with the doctor's data
     */
    private DoctorResponse mapToResponse(Doctor doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .name(doctor.getName())
                .specialization(doctor.getSpecialization())
                .email(doctor.getEmail())
                .phone(doctor.getPhone())
                .availability(doctor.isAvailability())
                .createdAt(doctor.getCreatedAt())
                .build();
    }
}
