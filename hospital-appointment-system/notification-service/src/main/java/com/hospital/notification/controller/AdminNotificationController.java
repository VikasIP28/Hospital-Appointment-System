package com.hospital.notification.controller;

import com.hospital.notification.dto.NotificationResponse;
import com.hospital.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Admin REST controller for notification management.
 *
 * Restricted to users with the ADMIN role. Provides administrative operations
 * including viewing all/failed notifications, resending failed emails, and
 * generating delivery reports.
 *
 * Endpoints:
 * - GET  /admin/notifications          -> List all notifications
 * - GET  /admin/notifications/failed   -> List failed (unsent) notifications
 * - POST /admin/notifications/{id}/resend -> Retry email for a specific notification
 * - GET  /admin/notifications/report   -> Get delivery statistics report
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationController {

    private final NotificationService notificationService;

    /**
     * Retrieves all notifications (admin view).
     * Same data as the /notifications endpoint but scoped under /admin for clarity.
     *
     * @return 200 OK with list of all notifications
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        log.info("ADMIN GET /admin/notifications - Retrieving all notifications");
        List<NotificationResponse> notifications = notificationService.getAllNotifications();
        log.info("Admin view: returning {} notifications", notifications.size());
        return ResponseEntity.ok(notifications);
    }

    /**
     * Retrieves notifications where email delivery failed (emailSent=false).
     * Admins can use this to identify notifications that need manual attention.
     *
     * @return 200 OK with list of failed notifications
     */
    @GetMapping("/notifications/failed")
    public ResponseEntity<List<NotificationResponse>> getFailedNotifications() {
        log.info("ADMIN GET /admin/notifications/failed - Retrieving failed notifications");
        List<NotificationResponse> failedNotifications = notificationService.getFailedNotifications();
        log.info("Found {} failed notifications", failedNotifications.size());
        return ResponseEntity.ok(failedNotifications);
    }

    /**
     * Retries sending the email for a specific failed notification.
     * The notification is looked up by ID, the email is re-attempted,
     * and the delivery status is updated.
     *
     * @param id the notification ID to resend
     * @return 200 OK with the updated notification (emailSent may now be true)
     */
    @PostMapping("/notifications/{id}/resend")
    public ResponseEntity<NotificationResponse> resendNotification(@PathVariable String id) {
        log.info("ADMIN POST /admin/notifications/{}/resend - Resending notification", id);
        NotificationResponse response = notificationService.resendNotification(id);
        log.info("Notification resend result: id={}, emailSent={}", id, response.isEmailSent());
        return ResponseEntity.ok(response);
    }

    /**
     * Generates and returns a delivery report with aggregated statistics.
     * Includes total count, sent/failed breakdown, success rate, and counts by type.
     *
     * @return 200 OK with the delivery report map
     */
    @GetMapping("/notifications/report")
    public ResponseEntity<Map<String, Object>> getNotificationDeliveryReport() {
        log.info("ADMIN GET /admin/notifications/report - Generating delivery report");
        Map<String, Object> report = notificationService.getNotificationDeliveryReport();
        log.info("Delivery report generated successfully");
        return ResponseEntity.ok(report);
    }
}
