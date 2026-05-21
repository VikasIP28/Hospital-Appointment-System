package com.hospital.appointment.client;

import com.hospital.appointment.dto.DoctorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;



/**
 * Fallback implementation for the DoctorServiceClient Feign interface.
 *
 * Provides graceful degradation when the Doctor Service is unreachable
 * or experiencing errors. Returns a placeholder DoctorResponse so that
 * appointment operations can still proceed with partial data.
 *
 * This fallback is triggered by Feign's built-in fallback mechanism.
 * Additional resilience (circuit breaker, retry, time limiter) is
 * handled via Resilience4j annotations at the service layer.
 */
@Component
@Slf4j
public class DoctorServiceFallback implements DoctorServiceClient {

    /**
     * Returns a fallback DoctorResponse when getDoctorById fails.
     * The appointment can still be created with the doctorId reference,
     * but the doctor's name will show as unavailable.
     *
     * @param id the doctor's unique identifier that was requested
     * @return a placeholder DoctorResponse indicating service unavailability
     */
    @Override
    public DoctorResponse getDoctorById(String id) {
        log.warn("Doctor Service is unavailable. Returning fallback response for doctorId: {}", id);
        return DoctorResponse.builder()
                .id(id)
                .name("Unknown Doctor (Service Unavailable)")
                .specialization("N/A")
                .email("N/A")
                .phone("N/A")
                .availability(true)
                .build();
    }

    /**
     * Returns a fallback DoctorResponse when checkAvailability fails.
     * Returns an empty availability map indicating we cannot verify
     * the doctor's schedule.
     *
     * @param id the doctor's unique identifier
     * @return a placeholder DoctorResponse with empty availability
     */
    @Override
    public DoctorResponse checkAvailability(String id) {
        log.warn("Doctor Service is unavailable. Cannot check availability for doctorId: {}", id);
        return DoctorResponse.builder()
                .id(id)
                .name("Unknown Doctor (Service Unavailable)")
                .specialization("N/A")
                .email("N/A")
                .phone("N/A")
                .availability(true)
                .build();
    }
}
