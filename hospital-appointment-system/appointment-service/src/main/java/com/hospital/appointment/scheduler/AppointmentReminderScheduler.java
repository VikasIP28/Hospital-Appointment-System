package com.hospital.appointment.scheduler;

import com.hospital.appointment.entity.Appointment;
import com.hospital.appointment.enums.AppointmentStatus;
import com.hospital.appointment.kafka.event.AppointmentEvent;
import com.hospital.appointment.kafka.producer.AppointmentEventProducer;
import com.hospital.appointment.mapper.AppointmentMapper;
import com.hospital.appointment.repository.AppointmentRepository;
import com.hospital.appointment.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================================
 * Appointment Reminder Scheduler
 * ============================================================================
 * Scheduled task that runs every hour (configurable via AppConstants)
 * to find upcoming CONFIRMED appointments within the next 24 hours
 * and publishes reminder events to the Kafka appointment-reminder topic.
 *
 * The Notification Service consumes these events and sends reminder
 * emails/notifications to patients about their upcoming appointments.
 *
 * Scheduling details:
 * - Runs every 1 hour (3,600,000 ms)
 * - Looks ahead 24 hours from current time
 * - Only considers CONFIRMED appointments (not PENDING, CANCELLED, etc.)
 * ============================================================================
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentEventProducer eventProducer;

    /**
     * Scheduled task that checks for upcoming confirmed appointments
     * and publishes reminder events to Kafka.
     *
     * Runs at a fixed rate defined by AppConstants.REMINDER_CHECK_INTERVAL_MS.
     */
    @Scheduled(fixedRate = AppConstants.REMINDER_CHECK_INTERVAL_MS)
    public void sendAppointmentReminders() {
        log.info("Appointment reminder scheduler triggered — scanning for upcoming appointments");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindow = now.plusHours(AppConstants.REMINDER_WINDOW_HOURS);

        // Find all CONFIRMED appointments within the reminder window
        List<Appointment> upcomingAppointments =
                appointmentRepository.findByDoctorIdAndStatus(null, AppointmentStatus.CONFIRMED);

        // Filter: only appointments in the reminder window
        // We query all confirmed then filter by date since MongoDB query
        // for date ranges requires a custom @Query or Criteria API
        List<Appointment> confirmedAppointments =
                appointmentRepository.findByStatus(AppointmentStatus.CONFIRMED);

        List<Appointment> remindable = confirmedAppointments.stream()
                .filter(apt -> apt.getAppointmentDate() != null)
                .filter(apt -> apt.getAppointmentDate().isAfter(now))
                .filter(apt -> apt.getAppointmentDate().isBefore(reminderWindow))
                .toList();

        if (remindable.isEmpty()) {
            log.info("No upcoming confirmed appointments found within the next {} hours",
                    AppConstants.REMINDER_WINDOW_HOURS);
            return;
        }

        log.info("Found {} upcoming appointment(s) requiring reminders", remindable.size());

        for (Appointment appointment : remindable) {
            try {
                AppointmentEvent event = AppointmentMapper.toEvent(appointment, AppConstants.EVENT_REMINDER);
                eventProducer.publishAppointmentReminder(event);
                log.info("Reminder event published for appointmentId='{}', date='{}'",
                        appointment.getId(), appointment.getAppointmentDate());
            } catch (Exception e) {
                log.error("Failed to publish reminder for appointmentId='{}': {}",
                        appointment.getId(), e.getMessage(), e);
            }
        }

        log.info("Appointment reminder check complete — {} reminder(s) published", remindable.size());
    }
}
