package com.hospital.doctor.controller;

import com.hospital.doctor.service.DoctorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ============================================================================
 * Admin Doctor Controller - Administrative REST API Endpoints
 * ============================================================================
 * Provides HTTP endpoints for administrative operations on doctor data.
 * Base path: /admin
 *
 * All endpoints in this controller require ADMIN role authorization.
 * This separation from DoctorController follows the Single Responsibility
 * Principle - admin-specific operations are isolated from public-facing APIs.
 *
 * Endpoints:
 * - GET /admin/analytics/doctors → Doctor workload analytics (ADMIN only)
 * ============================================================================
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminDoctorController {

    /** Service layer for doctor business logic */
    private final DoctorService doctorService;

    /**
     * Get doctor workload analytics.
     * Requires ADMIN role - provides aggregate statistics for administrative dashboards.
     *
     * Response includes:
     * - totalDoctors: Total number of registered doctors
     * - availableDoctors: Number of currently available doctors
     * - unavailableDoctors: Number of currently unavailable doctors
     * - doctorsBySpecialization: Map of specialization → doctor count
     *
     * @return 200 OK with analytics data map
     */
    @GetMapping("/analytics/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDoctorWorkloadAnalytics() {
        log.info("REST request to get doctor workload analytics (ADMIN)");
        Map<String, Object> analytics = doctorService.getDoctorWorkloadAnalytics();
        return ResponseEntity.ok(analytics);
    }
}
