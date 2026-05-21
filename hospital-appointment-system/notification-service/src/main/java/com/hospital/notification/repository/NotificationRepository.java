package com.hospital.notification.repository;

import com.hospital.notification.entity.Notification;
import com.hospital.notification.enums.NotificationType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MongoDB repository for Notification documents.
 *
 * Provides query methods for looking up notifications by various criteria
 * including appointment ID, recipient email, notification type, and delivery status.
 */
@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    /**
     * Find all notifications associated with a specific appointment.
     *
     * @param appointmentId the appointment identifier
     * @return list of notifications for the given appointment
     */
    List<Notification> findByAppointmentId(String appointmentId);

    /**
     * Find all notifications sent to a specific email address.
     *
     * @param recipientEmail the recipient's email address
     * @return list of notifications for the given recipient
     */
    List<Notification> findByRecipientEmail(String recipientEmail);

    /**
     * Find all notifications of a specific type (e.g., APPOINTMENT_CREATED, APPOINTMENT_CONFIRMED).
     *
     * @param notificationType the notification type to filter by
     * @return list of notifications matching the given type
     */
    List<Notification> findByNotificationType(NotificationType notificationType);

    /**
     * Find notifications by their email delivery status.
     * Pass false to find failed/pending notifications; true for successfully sent ones.
     *
     * @param emailSent whether the email was successfully sent
     * @return list of notifications matching the delivery status
     */
    List<Notification> findByEmailSent(boolean emailSent);

    /**
     * Count the number of notifications for a given type.
     * Used for generating delivery reports.
     *
     * @param notificationType the notification type to count
     * @return the count of notifications of that type
     */
    long countByNotificationType(NotificationType notificationType);

    /**
     * Count the number of notifications by their email delivery status.
     * Used for delivery reports (how many sent vs. failed).
     *
     * @param emailSent whether the email was successfully sent
     * @return the count of notifications matching the delivery status
     */
    long countByEmailSent(boolean emailSent);
}
