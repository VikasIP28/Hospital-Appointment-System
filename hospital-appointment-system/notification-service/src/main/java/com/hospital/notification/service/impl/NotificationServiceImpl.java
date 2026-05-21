package com.hospital.notification.service.impl;

import com.hospital.notification.dto.NotificationResponse;
import com.hospital.notification.entity.Notification;
import com.hospital.notification.enums.NotificationType;
import com.hospital.notification.exception.NotificationNotFoundException;
import com.hospital.notification.repository.NotificationRepository;
import com.hospital.notification.service.EmailService;
import com.hospital.notification.service.NotificationService;
import com.hospital.notification.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of NotificationService.
 *
 * Handles the core notification workflow:
 * 1. Receives notification requests from Kafka consumers
 * 2. Persists notification records to MongoDB
 * 3. Attempts email delivery via EmailService
 * 4. Tracks delivery status for admin monitoring
 *
 * Email subject lines are determined based on the NotificationType.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    /**
     * Creates a notification record and attempts to send the email.
     *
     * This method follows a two-phase save pattern:
     * Phase 1: Save the notification with emailSent=false (guarantees the record exists)
     * Phase 2: Attempt email delivery, then update emailSent and sentAt fields
     *
     * This ensures that even if email delivery fails, the notification is recorded
     * in the database and can be retried later.
     */
    @Override
    public NotificationResponse createAndSendNotification(String appointmentId, String recipientEmail,
                                                           String message, NotificationType type) {
        log.info("Creating notification: appointmentId={}, recipientEmail={}, type={}",
                appointmentId, recipientEmail, type);

        // Phase 1: Build and save the notification entity with emailSent=false
        Notification notification = Notification.builder()
                .appointmentId(appointmentId)
                .recipientEmail(recipientEmail)
                .message(message)
                .notificationType(type)
                .emailSent(false)
                .createdAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);
        log.debug("Notification record saved to MongoDB: id={}", notification.getId());

        // Phase 2: Attempt email delivery
        String subject = getEmailSubject(type);
        boolean emailSent = emailService.sendEmail(recipientEmail, subject, message);

        // Update the notification with the delivery result
        notification.setEmailSent(emailSent);
        if (emailSent) {
            notification.setSentAt(LocalDateTime.now());
            log.info("Email successfully sent for notification: id={}, to={}", notification.getId(), recipientEmail);
        } else {
            log.warn("Email was not sent for notification: id={}, to={} (disabled or failed)",
                    notification.getId(), recipientEmail);
        }

        // Save the updated delivery status
        notification = notificationRepository.save(notification);

        return mapToResponse(notification);
    }

    /**
     * Retrieves all notifications from the database.
     */
    @Override
    public List<NotificationResponse> getAllNotifications() {
        log.debug("Retrieving all notifications");
        return notificationRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves notifications for a specific appointment.
     */
    @Override
    public List<NotificationResponse> getByAppointmentId(String appointmentId) {
        log.debug("Retrieving notifications for appointmentId={}", appointmentId);
        return notificationRepository.findByAppointmentId(appointmentId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all notifications where email delivery failed (emailSent=false).
     * These are candidates for manual resending via the admin endpoint.
     */
    @Override
    public List<NotificationResponse> getFailedNotifications() {
        log.debug("Retrieving failed notifications (emailSent=false)");
        return notificationRepository.findByEmailSent(false)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retries sending the email for a specific notification.
     *
     * Looks up the notification by ID, attempts to resend the email using
     * the same recipient and message, and updates the delivery status.
     *
     * @throws NotificationNotFoundException if no notification exists with the given ID
     */
    @Override
    public NotificationResponse resendNotification(String id) {
        log.info("Attempting to resend notification: id={}", id);

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));

        // Determine the subject based on notification type
        String subject = getEmailSubject(notification.getNotificationType());

        // Attempt to resend the email
        boolean emailSent = emailService.sendEmail(
                notification.getRecipientEmail(),
                subject,
                notification.getMessage()
        );

        // Update the delivery status
        notification.setEmailSent(emailSent);
        if (emailSent) {
            notification.setSentAt(LocalDateTime.now());
            log.info("Email resend successful for notification: id={}", id);
        } else {
            log.warn("Email resend failed for notification: id={}", id);
        }

        notification = notificationRepository.save(notification);

        return mapToResponse(notification);
    }

    /**
     * Generates a comprehensive delivery report with statistics.
     *
     * The report includes:
     * - totalNotifications: total count of all notification records
     * - sentCount: number of notifications where email was successfully sent
     * - failedCount: number of notifications where email delivery failed
     * - countByType: breakdown of notification counts per NotificationType
     */
    @Override
    public Map<String, Object> getNotificationDeliveryReport() {
        log.info("Generating notification delivery report");

        Map<String, Object> report = new LinkedHashMap<>();

        // Overall counts
        long totalNotifications = notificationRepository.count();
        long sentCount = notificationRepository.countByEmailSent(true);
        long failedCount = notificationRepository.countByEmailSent(false);

        report.put("totalNotifications", totalNotifications);
        report.put("sentCount", sentCount);
        report.put("failedCount", failedCount);

        // Calculate delivery success rate
        if (totalNotifications > 0) {
            double successRate = (double) sentCount / totalNotifications * 100;
            report.put("deliverySuccessRate", String.format("%.2f%%", successRate));
        } else {
            report.put("deliverySuccessRate", "N/A");
        }

        // Count by notification type
        Map<String, Long> countByType = new HashMap<>();
        for (NotificationType type : NotificationType.values()) {
            long count = notificationRepository.countByNotificationType(type);
            countByType.put(type.name(), count);
        }
        report.put("countByType", countByType);

        log.info("Delivery report generated: total={}, sent={}, failed={}", totalNotifications, sentCount, failedCount);

        return report;
    }

    // ==========================================
    // Private Helper Methods
    // ==========================================

    /**
     * Determines the appropriate email subject line based on the notification type.
     *
     * @param type the NotificationType
     * @return the email subject string
     */
    private String getEmailSubject(NotificationType type) {
        return switch (type) {
            case APPOINTMENT_CREATED -> AppConstants.SUBJECT_APPOINTMENT_CREATED;
            case APPOINTMENT_CONFIRMED -> AppConstants.SUBJECT_APPOINTMENT_CONFIRMED;
            case APPOINTMENT_REJECTED -> AppConstants.SUBJECT_APPOINTMENT_REJECTED;
            case APPOINTMENT_REMINDER -> AppConstants.SUBJECT_APPOINTMENT_REMINDER;
            case APPOINTMENT_CANCELLED -> AppConstants.SUBJECT_APPOINTMENT_CANCELLED;
        };
    }

    /**
     * Maps a Notification entity to a NotificationResponse DTO.
     *
     * @param notification the entity to map
     * @return the corresponding DTO
     */
    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .appointmentId(notification.getAppointmentId())
                .recipientEmail(notification.getRecipientEmail())
                .message(notification.getMessage())
                .notificationType(notification.getNotificationType())
                .emailSent(notification.isEmailSent())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
