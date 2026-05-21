package com.hospital.notification.controller;

import com.hospital.notification.dto.NotificationResponse;
import com.hospital.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for notification query endpoints.
 *
 * Accessible to any authenticated user (patients, doctors, admins).
 * Provides read-only access to notification records.
 *
 * Endpoints:
 * - GET /notifications              -> Retrieve all notifications
 * - GET /notifications/appointment/{appointmentId} -> Retrieve by appointment
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Retrieves all notifications in the system.
     *
     * @return 200 OK with list of all notification records
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        log.info("GET /notifications - Retrieving all notifications");
        List<NotificationResponse> notifications = notificationService.getAllNotifications();
        log.info("Returning {} notifications", notifications.size());
        return ResponseEntity.ok(notifications);
    }

    /**
     * Retrieves all notifications for a specific appointment.
     * An appointment may have multiple notifications (created, confirmed, reminder, etc.).
     *
     * @param appointmentId the appointment identifier to query by
     * @return 200 OK with list of notifications for the appointment
     */
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<List<NotificationResponse>> getByAppointmentId(
            @PathVariable String appointmentId) {
        log.info("GET /notifications/appointment/{} - Retrieving notifications by appointment", appointmentId);
        List<NotificationResponse> notifications = notificationService.getByAppointmentId(appointmentId);
        log.info("Returning {} notifications for appointmentId={}", notifications.size(), appointmentId);
        return ResponseEntity.ok(notifications);
    }
}
