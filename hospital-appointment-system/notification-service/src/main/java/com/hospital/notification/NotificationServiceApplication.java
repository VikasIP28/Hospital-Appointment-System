package com.hospital.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Notification Service Application
 *
 * This microservice is responsible for:
 * 1. Consuming appointment-related Kafka events from the appointment-service
 * 2. Storing notification records in MongoDB for audit and tracking
 * 3. Sending actual email notifications to patients via SMTP (when enabled)
 * 4. Providing admin endpoints for monitoring notification delivery and retrying failures
 *
 * The service listens on port 8083 and uses MongoDB database 'notification_db'.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
