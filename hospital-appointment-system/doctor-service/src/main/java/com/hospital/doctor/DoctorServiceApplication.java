package com.hospital.doctor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================================
 * Doctor Service - Main Application Entry Point
 * ============================================================================
 * This microservice is responsible for managing doctor profiles in the
 * Hospital Appointment System. It provides REST APIs for:
 * - Creating and retrieving doctor profiles
 * - Checking and updating doctor availability
 * - Querying doctors by specialization
 * - Workload analytics for administrators
 * - Simulation endpoints for testing resilience patterns
 *
 * The service uses JWT-based authentication (validation only) and MongoDB
 * for data persistence. It runs on port 8082.
 * ============================================================================
 */
@SpringBootApplication
public class DoctorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DoctorServiceApplication.class, args);
    }
}
