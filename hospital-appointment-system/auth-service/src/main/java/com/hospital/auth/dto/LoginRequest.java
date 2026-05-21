package com.hospital.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ============================================================================
 * Login Request DTO
 * ============================================================================
 * Data Transfer Object for user login requests. Contains the email and
 * password credentials submitted by the client during authentication.
 *
 * Validation constraints ensure that:
 * - Email is not blank and follows a valid email format
 * - Password is not blank
 * ============================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * The user's email address used as the login identifier.
     * Must be a valid email format (e.g., "user@hospital.com").
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    /**
     * The user's plain-text password for authentication.
     * This will be compared against the BCrypt hash stored in the database.
     */
    @NotBlank(message = "Password is required")
    private String password;
}
