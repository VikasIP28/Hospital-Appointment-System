package com.hospital.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================================
 * Auth Service - Main Application Entry Point
 * ============================================================================
 * This is the primary Spring Boot application class for the Authentication
 * microservice. It bootstraps the application context, auto-configures all
 * Spring components (Web, Security, MongoDB, Actuator), and starts the
 * embedded Tomcat server on port 8084.
 *
 * Responsibilities of this service:
 * - User registration with role-based access (ADMIN, DOCTOR, PATIENT)
 * - User login with JWT token generation
 * - JWT token validation for inter-service authentication
 * - Password encoding using BCrypt
 * ============================================================================
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
