package com.hospital.appointment.config;

import com.hospital.appointment.util.AppConstants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic configuration for the Appointment Service.
 *
 * Auto-creates the required Kafka topics on application startup.
 * Each topic is configured with 3 partitions for parallelism and
 * 1 replica (suitable for development; increase for production).
 */
@Configuration
public class KafkaTopicConfig {

    /**
     * Topic for newly created appointment events.
     * Consumers: Notification Service (sends booking confirmation email)
     */
    @Bean
    public NewTopic appointmentCreatedTopic() {
        return TopicBuilder.name(AppConstants.TOPIC_APPOINTMENT_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic for confirmed appointment events.
     * Consumers: Notification Service (sends confirmation email to patient)
     */
    @Bean
    public NewTopic appointmentConfirmedTopic() {
        return TopicBuilder.name(AppConstants.TOPIC_APPOINTMENT_CONFIRMED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic for rejected appointment events.
     * Consumers: Notification Service (sends rejection email to patient)
     */
    @Bean
    public NewTopic appointmentRejectedTopic() {
        return TopicBuilder.name(AppConstants.TOPIC_APPOINTMENT_REJECTED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic for appointment reminder events.
     * Published by the AppointmentReminderScheduler for appointments
     * within the next 24 hours.
     * Consumers: Notification Service (sends reminder email/SMS)
     */
    @Bean
    public NewTopic appointmentReminderTopic() {
        return TopicBuilder.name(AppConstants.TOPIC_APPOINTMENT_REMINDER)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
