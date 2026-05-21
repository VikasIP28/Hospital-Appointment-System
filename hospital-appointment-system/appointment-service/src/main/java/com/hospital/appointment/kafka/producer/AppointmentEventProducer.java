package com.hospital.appointment.kafka.producer;

import com.hospital.appointment.kafka.event.AppointmentEvent;
import com.hospital.appointment.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka event producer for appointment lifecycle events.
 *
 * Publishes events to dedicated Kafka topics when appointments are
 * created, confirmed, rejected, or when reminders are triggered.
 * Also maintains an in-memory buffer of the last 100 events for
 * admin monitoring via the AdminController.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AppointmentEventProducer {

    private final KafkaTemplate<String, AppointmentEvent> kafkaTemplate;

    /**
     * In-memory ring buffer storing the last 100 published events.
     * Synchronized list to handle concurrent access from multiple threads.
     * Used by AdminController to expose recent Kafka activity.
     */
    private final List<AppointmentEvent> recentEvents =
            Collections.synchronizedList(new ArrayList<>());

    /** Maximum number of events to retain in the in-memory buffer */
    private static final int MAX_RECENT_EVENTS = 100;

    /**
     * Core method to publish an event to a specific Kafka topic.
     * Uses the appointmentId as the message key for consistent partitioning,
     * ensuring all events for the same appointment go to the same partition.
     *
     * @param topic the Kafka topic name
     * @param event the appointment event payload
     */
    public void publishEvent(String topic, AppointmentEvent event) {
        log.info("Publishing event to topic '{}': appointmentId={}, eventType={}, status={}",
                topic, event.getAppointmentId(), event.getEventType(), event.getStatus());

        CompletableFuture<SendResult<String, AppointmentEvent>> future =
                kafkaTemplate.send(topic, event.getAppointmentId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published successfully to topic '{}': partition={}, offset={}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event to topic '{}': appointmentId={}, error={}",
                        topic, event.getAppointmentId(), ex.getMessage(), ex);
            }
        });

        // Add to the in-memory recent events buffer (evict oldest if over capacity)
        addToRecentEvents(event);
    }

    /**
     * Publish an event when a new appointment is created.
     *
     * @param event the appointment created event
     */
    public void publishAppointmentCreated(AppointmentEvent event) {
        publishEvent(AppConstants.TOPIC_APPOINTMENT_CREATED, event);
    }

    /**
     * Publish an event when an appointment is confirmed by a doctor/admin.
     *
     * @param event the appointment confirmed event
     */
    public void publishAppointmentConfirmed(AppointmentEvent event) {
        publishEvent(AppConstants.TOPIC_APPOINTMENT_CONFIRMED, event);
    }

    /**
     * Publish an event when an appointment is rejected by a doctor/admin.
     *
     * @param event the appointment rejected event
     */
    public void publishAppointmentRejected(AppointmentEvent event) {
        publishEvent(AppConstants.TOPIC_APPOINTMENT_REJECTED, event);
    }

    /**
     * Publish a reminder event for an upcoming confirmed appointment.
     *
     * @param event the appointment reminder event
     */
    public void publishAppointmentReminder(AppointmentEvent event) {
        publishEvent(AppConstants.TOPIC_APPOINTMENT_REMINDER, event);
    }

    /**
     * Adds an event to the in-memory ring buffer, evicting the oldest
     * entry when the buffer exceeds MAX_RECENT_EVENTS.
     *
     * @param event the event to store
     */
    private void addToRecentEvents(AppointmentEvent event) {
        synchronized (recentEvents) {
            if (recentEvents.size() >= MAX_RECENT_EVENTS) {
                recentEvents.remove(0);
            }
            recentEvents.add(event);
        }
    }

    /**
     * Returns an unmodifiable snapshot of the recent events buffer.
     * Called by AdminController for the /admin/kafka/events endpoint.
     *
     * @return list of the most recent appointment events (up to 100)
     */
    public List<AppointmentEvent> getRecentEvents() {
        synchronized (recentEvents) {
            return new ArrayList<>(recentEvents);
        }
    }
}
