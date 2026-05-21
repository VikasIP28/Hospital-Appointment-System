package com.hospital.auth.controller;

import com.hospital.auth.dto.AuthResponse;
import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.RegisterRequest;
import com.hospital.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ============================================================================
 * Auth Controller - REST API Endpoints
 * ============================================================================
 * Exposes the authentication REST API for the Hospital Appointment System.
 * All endpoints are under the "/auth" base path and are publicly accessible
 * (no authentication required) as configured in SecurityConfig.
 *
 * Endpoints:
 * - POST /auth/register  - Register a new user account
 * - POST /auth/login     - Authenticate and get a JWT token
 * - GET  /auth/validate  - Validate a JWT token and get user info
 *
 * Request validation is handled via @Valid annotation which triggers
 * Jakarta Bean Validation constraints defined on the DTO classes.
 * Validation errors are caught by the GlobalExceptionHandler.
 * ============================================================================
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user account in the Hospital Appointment System.
     *
     * Accepts a JSON body with name, email, password, and role.
     * Returns a JWT token upon successful registration so the user
     * can immediately start making authenticated requests.
     *
     * Example request:
     * POST /auth/register
     * {
     *     "name": "Dr. John Smith",
     *     "email": "john.smith@hospital.com",
     *     "password": "securePassword123",
     *     "role": "DOCTOR"
     * }
     *
     * @param registerRequest the validated registration data
     * @return HTTP 201 with AuthResponse containing JWT token and user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("REST API: POST /auth/register - email: {}, role: {}",
                registerRequest.getEmail(), registerRequest.getRole());

        AuthResponse response = authService.register(registerRequest);

        log.info("REST API: Registration successful for email: {}", registerRequest.getEmail());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Authenticates a user with email and password credentials.
     *
     * Returns a JWT token upon successful authentication. The token
     * should be included in the Authorization header of subsequent
     * requests as: "Bearer <token>"
     *
     * Example request:
     * POST /auth/login
     * {
     *     "email": "john.smith@hospital.com",
     *     "password": "securePassword123"
     * }
     *
     * @param loginRequest the validated login credentials
     * @return HTTP 200 with AuthResponse containing JWT token and user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("REST API: POST /auth/login - email: {}", loginRequest.getEmail());

        AuthResponse response = authService.login(loginRequest);

        log.info("REST API: Login successful for email: {}", loginRequest.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * Validates a JWT token and returns the user information encoded in it.
     *
     * This endpoint is primarily used by the API Gateway and other
     * microservices to verify tokens and extract user details for
     * authorization decisions.
     *
     * Example request:
     * GET /auth/validate?token=eyJhbGciOiJIUzUxMiJ9...
     *
     * Example response:
     * {
     *     "valid": true,
     *     "email": "john.smith@hospital.com",
     *     "role": "DOCTOR"
     * }
     *
     * @param token the JWT token string to validate (passed as a query parameter)
     * @return HTTP 200 with a Map containing valid, email, and role fields
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam String token) {
        log.info("REST API: GET /auth/validate - token validation requested");

        Map<String, Object> response = authService.validateToken(token);

        log.info("REST API: Token validation result - valid: {}", response.get("valid"));
        return ResponseEntity.ok(response);
    }
}
