package com.hospital.doctor.controller;

import com.hospital.doctor.dto.DoctorAvailabilityResponse;
import com.hospital.doctor.dto.DoctorRequest;
import com.hospital.doctor.dto.DoctorResponse;
import com.hospital.doctor.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * ============================================================================
 * Doctor Controller - REST API Endpoints
 * ============================================================================
 * Provides HTTP endpoints for managing doctor profiles in the system.
 * Base path: /doctors
 *
 * Endpoints:
 * - POST   /doctors                        → Create a new doctor (DOCTOR/ADMIN only)
 * - GET    /doctors                        → List all doctors (public)
 * - GET    /doctors/{id}                   → Get doctor by ID (public)
 * - GET    /doctors/availability/{id}      → Check doctor availability (public)
 * - PUT    /doctors/availability/{id}      → Update doctor availability (DOCTOR/ADMIN only)
 * - GET    /doctors/specialization/{spec}  → Get doctors by specialization (public)
 * - GET    /doctors/simulate/slow          → Simulate slow response (testing)
 * - GET    /doctors/simulate/failure       → Simulate failure (testing)
 * - GET    /doctors/simulate/random        → Simulate random failure (testing)
 *
 * The simulation endpoints are used for testing resilience patterns
 * (circuit breakers, retries, timeouts) in the API Gateway.
 * ============================================================================
 */
@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
@Slf4j
public class DoctorController {

    /** Service layer for doctor business logic */
    private final DoctorService doctorService;

    /** Random number generator for simulation endpoints */
    private final Random random = new Random();

    /**
     * Create a new doctor profile.
     * Requires DOCTOR or ADMIN role (enforced by @PreAuthorize).
     * Validates the request body using Jakarta Bean Validation (@Valid).
     *
     * @param doctorRequest the doctor registration data
     * @return 201 Created with the saved doctor response
     */
    @PostMapping
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody DoctorRequest doctorRequest) {
        log.info("REST request to create doctor: {}", doctorRequest.getName());
        DoctorResponse response = doctorService.createDoctor(doctorRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all doctors in the system.
     * Publicly accessible - no authentication required.
     *
     * @return 200 OK with list of all doctor profiles
     */
    @GetMapping
    public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
        log.info("REST request to get all doctors");
        List<DoctorResponse> doctors = doctorService.getAllDoctors();
        return ResponseEntity.ok(doctors);
    }

    /**
     * Get a specific doctor by their unique ID.
     * Publicly accessible - no authentication required.
     *
     * @param id the doctor's unique identifier
     * @return 200 OK with the doctor profile, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponse> getDoctorById(@PathVariable String id) {
        log.info("REST request to get doctor by id: {}", id);
        DoctorResponse doctor = doctorService.getDoctorById(id);
        return ResponseEntity.ok(doctor);
    }

    /**
     * Check the availability of a specific doctor.
     * Returns a lightweight response with only availability-related fields.
     * Publicly accessible - used by appointment-service for availability checks.
     *
     * @param id the doctor's unique identifier
     * @return 200 OK with availability info, or 404 if not found
     */
    @GetMapping("/availability/{id}")
    public ResponseEntity<DoctorAvailabilityResponse> checkAvailability(@PathVariable String id) {
        log.info("REST request to check availability for doctor id: {}", id);
        DoctorAvailabilityResponse availability = doctorService.checkAvailability(id);
        return ResponseEntity.ok(availability);
    }

    /**
     * Update a doctor's availability status.
     * Requires DOCTOR or ADMIN role.
     *
     * @param id        the doctor's unique identifier
     * @param available the new availability status (query parameter)
     * @return 200 OK with the updated doctor profile
     */
    @PutMapping("/availability/{id}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<DoctorResponse> updateAvailability(
            @PathVariable String id,
            @RequestParam boolean available) {
        log.info("REST request to update availability for doctor id: {} to: {}", id, available);
        DoctorResponse response = doctorService.updateAvailability(id, available);
        return ResponseEntity.ok(response);
    }

    /**
     * Find all doctors with a specific medical specialization.
     * Publicly accessible - used for doctor search/filter functionality.
     *
     * @param specialization the specialization to filter by (e.g., "CARDIOLOGY")
     * @return 200 OK with list of matching doctors
     */
    @GetMapping("/specialization/{specialization}")
    public ResponseEntity<List<DoctorResponse>> getDoctorsBySpecialization(
            @PathVariable String specialization) {
        log.info("REST request to get doctors by specialization: {}", specialization);
        List<DoctorResponse> doctors = doctorService.getDoctorsBySpecialization(specialization);
        return ResponseEntity.ok(doctors);
    }

    // ========================================================================
    // Simulation Endpoints - For Testing Resilience Patterns
    // ========================================================================
    // These endpoints deliberately introduce delays, failures, and random errors
    // to test how upstream services (API Gateway, other microservices) handle
    // degraded conditions using circuit breakers, retries, and timeouts.
    // ========================================================================

    /**
     * Simulate a slow response (5-second delay).
     * Used to test timeout configurations in the API Gateway.
     * The 5-second delay simulates a database query or external service
     * call that takes too long to respond.
     *
     * @return 200 OK with a delayed response message
     * @throws InterruptedException if the sleep is interrupted
     */
    @GetMapping("/simulate/slow")
    public ResponseEntity<Map<String, String>> simulateSlowResponse() throws InterruptedException {
        log.warn("Simulating slow response - sleeping for 5 seconds...");
        Thread.sleep(5000); // 5-second delay
        log.info("Slow response completed");
        return ResponseEntity.ok(Map.of(
                "message", "This response was delayed by 5 seconds",
                "status", "success"
        ));
    }

    /**
     * Simulate a complete failure (RuntimeException).
     * Used to test circuit breaker and fallback configurations.
     * Always throws an exception to simulate a service crash.
     *
     * @return never returns - always throws RuntimeException
     */
    @GetMapping("/simulate/failure")
    public ResponseEntity<Map<String, String>> simulateFailure() {
        log.error("Simulating service failure - throwing RuntimeException");
        throw new RuntimeException("Simulated service failure for testing resilience patterns");
    }

    /**
     * Simulate random failures (50% chance of failure).
     * Used to test retry mechanisms and circuit breaker thresholds.
     * Randomly succeeds or fails to create an unpredictable failure pattern.
     *
     * @return 200 OK on success (50% chance), or throws RuntimeException (50% chance)
     */
    @GetMapping("/simulate/random")
    public ResponseEntity<Map<String, String>> simulateRandomFailure() {
        // 50% chance of failure
        if (random.nextBoolean()) {
            log.error("Random simulation: FAILURE triggered");
            throw new RuntimeException("Random failure occurred! (50% chance simulation)");
        }
        log.info("Random simulation: SUCCESS");
        return ResponseEntity.ok(Map.of(
                "message", "Random simulation succeeded!",
                "status", "success"
        ));
    }
}
