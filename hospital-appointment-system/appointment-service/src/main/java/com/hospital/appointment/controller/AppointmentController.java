package com.hospital.appointment.controller;

import com.hospital.appointment.dto.AppointmentRequest;
import com.hospital.appointment.dto.AppointmentResponse;
import com.hospital.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ============================================================================
 * Appointment REST Controller
 * ============================================================================
 * Exposes REST endpoints for appointment lifecycle management.
 *
 * Endpoints:
 *   POST   /appointments/create                  — Create a new appointment (PATIENT)
 *   GET    /appointments/get                     — List all appointments
 *   GET    /appointments/{id}                    — Get appointment by ID
 *   PUT    /appointments/{id}/confirm            — Confirm appointment (DOCTOR/ADMIN)
 *   PUT    /appointments/{id}/reject             — Reject appointment (DOCTOR/ADMIN)
 *   PUT    /appointments/{id}/cancel             — Cancel appointment (PATIENT)
 *   GET    /appointments/doctor/{doctorId}/pending — Pending appointments for a doctor
 *
 * Access control is enforced via @PreAuthorize with role-based checks.
 * ============================================================================
 */
@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * Creates a new appointment.
     * Any authenticated user (typically PATIENT) can create an appointment.
     *
     * @param request validated appointment creation request
     * @return 201 Created with the appointment response
     */
    @PostMapping("/create")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody AppointmentRequest request) {
        log.info("POST /appointments — patient='{}', doctorId='{}', date='{}'",
                request.getPatientName(), request.getDoctorId(), request.getAppointmentDate());

        AppointmentResponse response = appointmentService.createAppointment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Retrieves all appointments in the system.
     *
     * @return 200 OK with list of all appointments
     */
    @GetMapping("/get")
    public ResponseEntity<List<AppointmentResponse>> getAllAppointments() {
        log.info("GET /appointments — fetching all appointments");
        List<AppointmentResponse> appointments = appointmentService.getAllAppointments();
        return ResponseEntity.ok(appointments);
    }

    /**
     * Retrieves a single appointment by its unique ID.
     *
     * @param id the appointment ID
     * @return 200 OK with the appointment, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable String id) {
        log.info("GET /appointments/{} — fetching appointment", id);
        AppointmentResponse response = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Confirms a pending appointment. Only DOCTOR or ADMIN roles allowed.
     *
     * @param id the appointment ID to confirm
     * @return 200 OK with the updated appointment
     */
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> confirmAppointment(@PathVariable String id) {
        log.info("PUT /appointments/{}/confirm — confirming appointment", id);
        AppointmentResponse response = appointmentService.confirmAppointment(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Rejects a pending appointment. Only DOCTOR or ADMIN roles allowed.
     *
     * @param id the appointment ID to reject
     * @return 200 OK with the updated appointment
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> rejectAppointment(@PathVariable String id) {
        log.info("PUT /appointments/{}/reject — rejecting appointment", id);
        AppointmentResponse response = appointmentService.rejectAppointment(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels an appointment. Any authenticated user can cancel.
     *
     * @param id the appointment ID to cancel
     * @return 200 OK with the updated appointment
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(@PathVariable String id) {
        log.info("PUT /appointments/{}/cancel — cancelling appointment", id);
        AppointmentResponse response = appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all PENDING appointments for a specific doctor.
     * Used by doctors to see their incoming appointment requests.
     *
     * @param doctorId the doctor's unique ID
     * @return 200 OK with list of pending appointments
     */
    @GetMapping("/doctor/{doctorId}/pending")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<AppointmentResponse>> getPendingAppointments(
            @PathVariable String doctorId) {
        log.info("GET /appointments/doctor/{}/pending — fetching pending appointments", doctorId);
        List<AppointmentResponse> pending = appointmentService.getPendingAppointmentsByDoctorId(doctorId);
        return ResponseEntity.ok(pending);
    }
}
