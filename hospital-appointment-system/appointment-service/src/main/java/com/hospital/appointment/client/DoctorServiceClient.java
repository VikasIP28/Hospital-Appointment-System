package com.hospital.appointment.client;

import com.hospital.appointment.dto.DoctorResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign declarative REST client for communicating with the Doctor Service.
 *
 * When the Doctor Service is unavailable, the DoctorServiceFallback class
 * provides graceful degradation. Resilience4j annotations are applied
 * at the service layer (AppointmentServiceImpl) rather than here,
 * following the recommended pattern for circuit breaker + retry composition.
 */
@FeignClient(
        name = "doctor-service",
        url = "${doctor.service.url}",
        fallback = DoctorServiceFallback.class
)
public interface DoctorServiceClient {

    /**
     * Fetches complete doctor information by their unique ID.
     *
     * @param id the doctor's unique identifier
     * @return DoctorResponse containing the doctor's details
     */
    @GetMapping("/doctors/{id}")
    DoctorResponse getDoctorById(@PathVariable("id") String id);

    /**
     * Checks a doctor's availability schedule by their ID.
     *
     * @param id the doctor's unique identifier
     * @return DoctorResponse with availability information populated
     */
    @GetMapping("/doctors/availability/{id}")
    DoctorResponse checkAvailability(@PathVariable("id") String id);
}
