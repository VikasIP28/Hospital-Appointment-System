package com.hospital.notification.kafka.consumer;

import com.hospital.notification.enums.NotificationType;
import com.hospital.notification.kafka.event.AppointmentEvent;
import com.hospital.notification.service.NotificationService;
import com.hospital.notification.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka event consumer for appointment lifecycle events.
 *
 * Listens on multiple Kafka topics corresponding to different appointment events:
 * - appointment-created: new appointment was booked by a patient
 * - appointment-confirmed: doctor confirmed a pending appointment
 * - appointment-rejected: doctor rejected a pending appointment
 * - appointment-reminder: scheduled reminder for an upcoming appointment
 *
 * Each handler method:
 * 1. Logs the received event with all details
 * 2. Builds a human-readable notification message using templates from AppConstants
 * 3. Delegates to NotificationService to persist the notification and attempt email delivery
 * 4. Logs the processing result
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AppointmentEventConsumer {

    private final NotificationService notificationService;

    /**
     * Handles newly created appointment events.
     * Sends a notification informing the patient that their appointment has been created
     * and is awaiting doctor confirmation.
     *
     * @param event the appointment creation event from Kafka
     */
    @KafkaListener(
            topics = AppConstants.TOPIC_APPOINTMENT_CREATED,
            groupId = AppConstants.KAFKA_GROUP_ID
    )
    public void handleAppointmentCreated(AppointmentEvent event) {
        log.info("Received appointment-created event: appointmentId={}, patientName={}, patientEmail={}, date={}",
                event.getAppointmentId(), event.getPatientName(), event.getPatientEmail(), event.getAppointmentDate());

        try {
            // Build the notification message from the template
            String message = String.format(
                    AppConstants.MSG_APPOINTMENT_CREATED,
                    event.getPatientName(),
                    event.getAppointmentDate(),
                    event.getAppointmentId()
            );

            // Create notification record and attempt email delivery
            notificationService.createAndSendNotification(
                    event.getAppointmentId(),
                    event.getPatientEmail(),
                    message,
                    NotificationType.APPOINTMENT_CREATED
            );

            log.info("Successfully processed appointment-created event for appointmentId={}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("Error processing appointment-created event for appointmentId={}: {}",
                    event.getAppointmentId(), e.getMessage(), e);
        }
    }

    /**
     * Handles appointment confirmation events.
     * Sends a notification informing the patient that their appointment has been
     * confirmed by the doctor and they should arrive early.
     *
     * @param event the appointment confirmation event from Kafka
     */
    @KafkaListener(
            topics = AppConstants.TOPIC_APPOINTMENT_CONFIRMED,
            groupId = AppConstants.KAFKA_GROUP_ID
    )
    public void handleAppointmentConfirmed(AppointmentEvent event) {
        log.info("Received appointment-confirmed event: appointmentId={}, patientName={}, patientEmail={}, date={}",
                event.getAppointmentId(), event.getPatientName(), event.getPatientEmail(), event.getAppointmentDate());

        try {
            String message = String.format(
                    AppConstants.MSG_APPOINTMENT_CONFIRMED,
                    event.getPatientName(),
                    event.getAppointmentId(),
                    event.getAppointmentDate()
            );

            notificationService.createAndSendNotification(
                    event.getAppointmentId(),
                    event.getPatientEmail(),
                    message,
                    NotificationType.APPOINTMENT_CONFIRMED
            );

            log.info("Successfully processed appointment-confirmed event for appointmentId={}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("Error processing appointment-confirmed event for appointmentId={}: {}",
                    event.getAppointmentId(), e.getMessage(), e);
        }
    }

    /**
     * Handles appointment rejection events.
     * Sends a notification informing the patient that their appointment was rejected
     * and they should rebook with a different time or doctor.
     *
     * @param event the appointment rejection event from Kafka
     */
    @KafkaListener(
            topics = AppConstants.TOPIC_APPOINTMENT_REJECTED,
            groupId = AppConstants.KAFKA_GROUP_ID
    )
    public void handleAppointmentRejected(AppointmentEvent event) {
        log.info("Received appointment-rejected event: appointmentId={}, patientName={}, patientEmail={}",
                event.getAppointmentId(), event.getPatientName(), event.getPatientEmail());

        try {
            String message = String.format(
                    AppConstants.MSG_APPOINTMENT_REJECTED,
                    event.getPatientName(),
                    event.getAppointmentId()
            );

            notificationService.createAndSendNotification(
                    event.getAppointmentId(),
                    event.getPatientEmail(),
                    message,
                    NotificationType.APPOINTMENT_REJECTED
            );

            log.info("Successfully processed appointment-rejected event for appointmentId={}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("Error processing appointment-rejected event for appointmentId={}: {}",
                    event.getAppointmentId(), e.getMessage(), e);
        }
    }

    /**
     * Handles appointment reminder events.
     * Sends a reminder notification to the patient about their upcoming appointment.
     *
     * @param event the appointment reminder event from Kafka
     */
    @KafkaListener(
            topics = AppConstants.TOPIC_APPOINTMENT_REMINDER,
            groupId = AppConstants.KAFKA_GROUP_ID
    )
    public void handleAppointmentReminder(AppointmentEvent event) {
        log.info("Received appointment-reminder event: appointmentId={}, patientName={}, patientEmail={}, date={}",
                event.getAppointmentId(), event.getPatientName(), event.getPatientEmail(), event.getAppointmentDate());

        try {
            String message = String.format(
                    AppConstants.MSG_APPOINTMENT_REMINDER,
                    event.getPatientName(),
                    event.getAppointmentId(),
                    event.getAppointmentDate()
            );

            notificationService.createAndSendNotification(
                    event.getAppointmentId(),
                    event.getPatientEmail(),
                    message,
                    NotificationType.APPOINTMENT_REMINDER
            );

            log.info("Successfully processed appointment-reminder event for appointmentId={}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("Error processing appointment-reminder event for appointmentId={}: {}",
                    event.getAppointmentId(), e.getMessage(), e);
        }
    }
}
