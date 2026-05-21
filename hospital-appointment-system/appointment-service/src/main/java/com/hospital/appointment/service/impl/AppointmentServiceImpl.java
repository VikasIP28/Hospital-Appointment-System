package com.hospital.appointment.service.impl;

import com.hospital.appointment.client.DoctorServiceClient;
import com.hospital.appointment.dto.AppointmentRequest;
import com.hospital.appointment.dto.AppointmentResponse;
import com.hospital.appointment.dto.DoctorResponse;
import com.hospital.appointment.entity.Appointment;
import com.hospital.appointment.enums.AppointmentStatus;
import com.hospital.appointment.exception.AppointmentNotFoundException;
import com.hospital.appointment.exception.InvalidStatusTransitionException;
import com.hospital.appointment.kafka.event.AppointmentEvent;
import com.hospital.appointment.kafka.producer.AppointmentEventProducer;
import com.hospital.appointment.mapper.AppointmentMapper;
import com.hospital.appointment.repository.AppointmentRepository;
import com.hospital.appointment.service.AppointmentService;
import com.hospital.appointment.util.AppConstants;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * Appointment Service Implementation
 * ============================================================================
 * Core business logic for the appointment lifecycle. This service:
 *
 * 1. Calls Doctor Service via Feign (protected by Resilience4j)
 * 2. Persists appointments to MongoDB
 * 3. Publishes lifecycle events to Kafka topics
 *
 * Resilience4j patterns applied to Doctor Service calls:
 * - @CircuitBreaker: Opens after 50% failure rate in sliding window of 10
 * - @Retry: Up to 3 attempts with exponential backoff (2s, 4s, 8s)
 * - Fallback: Returns a default DoctorResponse when service is unavailable
 * ============================================================================
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorServiceClient doctorServiceClient;
    private final AppointmentEventProducer eventProducer;

    @Lazy
    @Autowired
    private AppointmentServiceImpl self;

    // ========================================================================
    // Resilience4j-protected Doctor Service call
    // ========================================================================

    /**
     * Calls Doctor Service with Circuit Breaker and Retry protection.
     * The @CircuitBreaker annotation monitors failure rates and opens the
     * circuit when the threshold is exceeded, preventing cascading failures.
     * The @Retry annotation automatically retries failed calls with
     * exponential backoff.
     *
     * @param doctorId the doctor's unique ID
     * @return DoctorResponse from Doctor Service, or fallback response
     */
    @CircuitBreaker(name = AppConstants.CB_DOCTOR_SERVICE, fallbackMethod = "doctorServiceFallback")
    @Retry(name = AppConstants.CB_DOCTOR_SERVICE, fallbackMethod = "doctorServiceFallback")
    public DoctorResponse getDoctorWithResilience(String doctorId) {
        log.info("Calling Doctor Service for doctorId={} (with CircuitBreaker + Retry)", doctorId);
        return doctorServiceClient.getDoctorById(doctorId);
    }

    /**
     * Fallback method invoked when Doctor Service is unavailable.
     * Returns a default DoctorResponse so appointment creation can proceed
     * even when Doctor Service is down (graceful degradation).
     *
     * @param doctorId the doctor's ID that was requested
     * @param ex       the exception that triggered the fallback
     * @return a fallback DoctorResponse with placeholder values
     */
    public DoctorResponse doctorServiceFallback(String doctorId, Exception ex) {
        log.warn("Doctor Service fallback triggered for doctorId={}: {} - {}",
                doctorId, ex.getClass().getSimpleName(), ex.getMessage());

        return DoctorResponse.builder()
                .id(doctorId)
                .name("Unknown Doctor (Service Unavailable)")
                .specialization("N/A")
                .email("N/A")
                .availability(true) // Assume available to not block appointment creation
                .build();
    }

    // ========================================================================
    // CRUD Operations
    // ========================================================================

    /**
     * Creates a new appointment:
     * 1. Validates the doctor via Doctor Service (resilient call)
     * 2. Maps request to entity with PENDING status
     * 3. Saves to MongoDB
     * 4. Publishes appointment-created Kafka event
     */
    @Override
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        log.info("Creating appointment for patient='{}', doctorId='{}', date='{}'",
                request.getPatientName(), request.getDoctorId(), request.getAppointmentDate());

        // Step 1: Validate doctor exists (resilient call with circuit breaker)
        DoctorResponse doctor = self.getDoctorWithResilience(request.getDoctorId());
        log.info("Doctor resolved: name='{}', specialization='{}'", doctor.getName(), doctor.getSpecialization());

        // Step 2: Map request to entity and persist
        Appointment appointment = AppointmentMapper.toEntity(request);
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment saved with id='{}', status='{}'", saved.getId(), saved.getStatus());

        // Step 3: Publish Kafka event
        AppointmentEvent event = AppointmentMapper.toEvent(saved, AppConstants.EVENT_CREATED);
        eventProducer.publishAppointmentCreated(event);

        // Step 4: Return enriched response with doctor name
        return AppointmentMapper.toResponse(saved, doctor.getName());
    }

    @Override
    public List<AppointmentResponse> getAllAppointments() {
        log.debug("Fetching all appointments");
        return appointmentRepository.findAll().stream()
                .map(AppointmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AppointmentResponse getAppointmentById(String id) {
        log.debug("Fetching appointment by id='{}'", id);
        Appointment appointment = findAppointmentOrThrow(id);
        return AppointmentMapper.toResponse(appointment);
    }

    // ========================================================================
    // Status Transitions
    // ========================================================================

    /**
     * Confirms a PENDING appointment → CONFIRMED.
     * Publishes appointment-confirmed Kafka event.
     */
    @Override
    public AppointmentResponse confirmAppointment(String id) {
        log.info("Confirming appointment id='{}'", id);
        Appointment appointment = findAppointmentOrThrow(id);

        validateStatusTransition(appointment, AppointmentStatus.CONFIRMED);

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Appointment updated = appointmentRepository.save(appointment);
        log.info("Appointment id='{}' confirmed successfully", id);

        // Publish confirmed event
        AppointmentEvent event = AppointmentMapper.toEvent(updated, AppConstants.EVENT_CONFIRMED);
        eventProducer.publishAppointmentConfirmed(event);

        return AppointmentMapper.toResponse(updated);
    }

    /**
     * Rejects a PENDING appointment → REJECTED.
     * Publishes appointment-rejected Kafka event.
     */
    @Override
    public AppointmentResponse rejectAppointment(String id) {
        log.info("Rejecting appointment id='{}'", id);
        Appointment appointment = findAppointmentOrThrow(id);

        validateStatusTransition(appointment, AppointmentStatus.REJECTED);

        appointment.setStatus(AppointmentStatus.REJECTED);
        Appointment updated = appointmentRepository.save(appointment);
        log.info("Appointment id='{}' rejected successfully", id);

        // Publish rejected event
        AppointmentEvent event = AppointmentMapper.toEvent(updated, AppConstants.EVENT_REJECTED);
        eventProducer.publishAppointmentRejected(event);

        return AppointmentMapper.toResponse(updated);
    }

    /**
     * Cancels an appointment → CANCELLED.
     * Only PENDING and CONFIRMED appointments can be cancelled.
     */
    @Override
    public AppointmentResponse cancelAppointment(String id) {
        log.info("Cancelling appointment id='{}'", id);
        Appointment appointment = findAppointmentOrThrow(id);

        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
            appointment.getStatus() == AppointmentStatus.CANCELLED ||
            appointment.getStatus() == AppointmentStatus.REJECTED) {
            throw new InvalidStatusTransitionException(
                    String.format("Cannot cancel appointment with status '%s'", appointment.getStatus()));
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment updated = appointmentRepository.save(appointment);
        log.info("Appointment id='{}' cancelled successfully", id);

        return AppointmentMapper.toResponse(updated);
    }

    // ========================================================================
    // Queries
    // ========================================================================

    @Override
    public List<AppointmentResponse> getPendingAppointmentsByDoctorId(String doctorId) {
        log.debug("Fetching pending appointments for doctorId='{}'", doctorId);
        return appointmentRepository.findByDoctorIdAndStatus(doctorId, AppointmentStatus.PENDING)
                .stream()
                .map(AppointmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // Analytics
    // ========================================================================

    /**
     * Generates aggregated statistics for the admin dashboard.
     */
    @Override
    public Map<String, Object> getAppointmentStatistics() {
        log.info("Generating appointment statistics");
        Map<String, Object> stats = new HashMap<>();

        long total = appointmentRepository.count();
        stats.put("totalAppointments", total);

        // Count by each status
        for (AppointmentStatus status : AppointmentStatus.values()) {
            long count = appointmentRepository.countByStatus(status);
            stats.put(status.name().toLowerCase() + "Count", count);
        }

        // Appointments created today
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        long todayCount = appointmentRepository.findAll().stream()
                .filter(a -> a.getCreatedAt() != null
                        && !a.getCreatedAt().isBefore(startOfDay)
                        && !a.getCreatedAt().isAfter(endOfDay))
                .count();
        stats.put("appointmentsToday", todayCount);

        log.info("Statistics generated: {}", stats);
        return stats;
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Finds an appointment by ID or throws AppointmentNotFoundException.
     */
    private Appointment findAppointmentOrThrow(String id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(
                        "Appointment not found with id: " + id));
    }

    /**
     * Validates that the current status allows transition to the target status.
     * Only PENDING appointments can be confirmed or rejected.
     */
    private void validateStatusTransition(Appointment appointment, AppointmentStatus targetStatus) {
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new InvalidStatusTransitionException(
                    String.format("Cannot transition from '%s' to '%s'. Only PENDING appointments can be %s.",
                            appointment.getStatus(), targetStatus,
                            targetStatus == AppointmentStatus.CONFIRMED ? "confirmed" : "rejected"));
        }
    }
}
