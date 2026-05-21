package com.hospital.notification.entity;

import com.hospital.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB document representing a notification record.
 *
 * Each notification is created when a Kafka event is consumed from the appointment-service.
 * It tracks:
 * - What appointment triggered it (appointmentId)
 * - Who should receive the email (recipientEmail)
 * - The message content and notification type
 * - Whether the email was successfully sent (emailSent flag)
 * - Timestamps for creation and sending
 *
 * Indexes on appointmentId and recipientEmail enable efficient lookups.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class Notification {

    /** Unique identifier for the notification document */
    @Id
    private String id;

    /** The appointment that triggered this notification */
    @Indexed
    private String appointmentId;

    /** Email address of the notification recipient (typically the patient) */
    @Indexed
    private String recipientEmail;

    /** The notification message body content */
    private String message;

    /** Type/category of this notification (created, confirmed, rejected, etc.) */
    @Indexed
    private NotificationType notificationType;

    /** Flag indicating whether the email was successfully delivered via SMTP */
    @Builder.Default
    private boolean emailSent = false;

    /** Timestamp when the email was successfully sent (null if not yet sent) */
    private LocalDateTime sentAt;

    /** Timestamp when this notification record was created in the database */
    @CreatedDate
    private LocalDateTime createdAt;
}
