package com.hospital.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway Application - Entry Point
 * ======================================
 * This is the main entry point for the API Gateway microservice.
 * The gateway acts as a single entry point for all client requests,
 * handling JWT validation and routing requests to the appropriate
 * downstream microservices:
 *
 *   - Auth Service       (port 8084) - Authentication & registration
 *   - Appointment Service (port 8081) - Appointment management
 *   - Doctor Service      (port 8082) - Doctor profiles & availability
 *   - Notification Service (port 8083) - Email/SMS notifications
 *
 * This gateway uses Spring Boot Web (imperative/servlet-based) rather
 * than Spring Cloud Gateway (reactive/WebFlux-based) for simplicity
 * and compatibility with the rest of the system.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
