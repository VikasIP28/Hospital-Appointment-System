package com.hospital.notification.dto;

import com.hospital.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for returning notification data in API responses.
 * Maps all fields from the Notification entity for external consumption.
 * Uses @Builder pattern for clean construction from entity objects.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    /** Unique notification identifier */
    private String id;

    /** The appointment that triggered this notification */
    private String appointmentId;

    /** Email address of the notification recipient */
    private String recipientEmail;

    /** The notification message content */
    private String message;

    /** Type of notification (APPOINTMENT_CREATED, CONFIRMED, REJECTED, etc.) */
    private NotificationType notificationType;

    /** Whether the email was successfully delivered */
    private boolean emailSent;

    /** Timestamp when the email was successfully sent */
    private LocalDateTime sentAt;

    /** Timestamp when the notification was created */
    private LocalDateTime createdAt;
}
