package com.hospital.notification.service;

import com.hospital.notification.dto.NotificationResponse;
import com.hospital.notification.enums.NotificationType;

import java.util.List;
import java.util.Map;

/**
 * Service interface for notification management operations.
 *
 * Defines the contract for:
 * - Creating and sending notifications (triggered by Kafka events)
 * - Querying notifications by various criteria
 * - Admin operations like resending failed notifications and generating delivery reports
 */
public interface NotificationService {

    /**
     * Creates a notification record in MongoDB and attempts to send an email.
     * This is the primary method called by Kafka event consumers.
     *
     * Flow:
     * 1. Build a Notification entity with the provided details
     * 2. Save it to MongoDB (initially with emailSent=false)
     * 3. Attempt to send the email via EmailService
     * 4. Update the emailSent flag and sentAt timestamp based on the result
     * 5. Save the updated notification
     *
     * @param appointmentId  the appointment that triggered this notification
     * @param recipientEmail the email address to send the notification to
     * @param message        the notification message body
     * @param type           the type of notification (CREATED, CONFIRMED, etc.)
     * @return the created NotificationResponse
     */
    NotificationResponse createAndSendNotification(String appointmentId, String recipientEmail,
                                                    String message, NotificationType type);

    /**
     * Retrieves all notifications in the system.
     *
     * @return list of all notifications
     */
    List<NotificationResponse> getAllNotifications();

    /**
     * Retrieves all notifications for a specific appointment.
     *
     * @param appointmentId the appointment identifier
     * @return list of notifications for the appointment
     */
    List<NotificationResponse> getByAppointmentId(String appointmentId);

    /**
     * Retrieves all notifications that failed to send (emailSent=false).
     * Used by admins to identify and retry failed deliveries.
     *
     * @return list of notifications where email delivery failed
     */
    List<NotificationResponse> getFailedNotifications();

    /**
     * Retries sending the email for a previously failed notification.
     * Looks up the notification by ID, attempts to resend the email,
     * and updates the delivery status.
     *
     * @param id the notification ID to retry
     * @return the updated NotificationResponse
     */
    NotificationResponse resendNotification(String id);

    /**
     * Generates a delivery report with aggregated statistics.
     * Includes total count, sent/failed breakdown, and counts by notification type.
     *
     * @return map containing report data
     */
    Map<String, Object> getNotificationDeliveryReport();
}
