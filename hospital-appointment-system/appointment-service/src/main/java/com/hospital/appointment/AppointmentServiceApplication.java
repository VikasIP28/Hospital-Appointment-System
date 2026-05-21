package com.hospital.appointment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Appointment Service microservice.
 *
 * This service handles all appointment-related operations including:
 * - Creating, confirming, rejecting, and cancelling appointments
 * - Communicating with Doctor Service via Feign clients with Resilience4j patterns
 * - Publishing appointment lifecycle events to Kafka topics
 * - Scheduling appointment reminders
 *
 * @EnableFeignClients  - Enables declarative REST clients for inter-service communication
 * @EnableScheduling    - Enables scheduled tasks (e.g., appointment reminders)
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class AppointmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppointmentServiceApplication.class, args);
    }
}
