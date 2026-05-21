package com.hospital.doctor.repository;

import com.hospital.doctor.entity.Doctor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * Doctor Repository - MongoDB Data Access Layer
 * ============================================================================
 * Provides CRUD operations and custom query methods for Doctor documents.
 * Extends MongoRepository which provides built-in methods like:
 * - save(), findById(), findAll(), deleteById(), count(), etc.
 *
 * Custom query methods leverage Spring Data MongoDB's query derivation,
 * automatically generating queries from method names.
 * ============================================================================
 */
@Repository
public interface DoctorRepository extends MongoRepository<Doctor, String> {

    /**
     * Find a doctor by their email address.
     * Email is unique, so this returns an Optional (0 or 1 result).
     *
     * @param email the doctor's email address
     * @return Optional containing the doctor if found, empty otherwise
     */
    Optional<Doctor> findByEmail(String email);

    /**
     * Find all doctors with a given specialization.
     * Used to filter doctors by their medical specialty (e.g., "CARDIOLOGY").
     *
     * @param specialization the medical specialization to search for
     * @return list of doctors with the specified specialization
     */
    List<Doctor> findBySpecialization(String specialization);

    /**
     * Find all doctors by their availability status.
     * Used to get all currently available (or unavailable) doctors.
     *
     * @param availability true for available doctors, false for unavailable
     * @return list of doctors matching the availability status
     */
    List<Doctor> findByAvailability(boolean availability);
}
