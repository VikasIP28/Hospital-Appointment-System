package com.hospital.auth.dto;

import com.hospital.auth.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ============================================================================
 * Register Request DTO
 * ============================================================================
 * Data Transfer Object for user registration requests. Contains all the
 * information needed to create a new user account in the system.
 *
 * Validation constraints ensure that:
 * - Name is not blank
 * - Email is not blank and follows a valid email format
 * - Password is not blank and has a minimum length of 6 characters
 * - Role is not null and must be one of ADMIN, DOCTOR, or PATIENT
 * ============================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /**
     * The user's full display name (e.g., "Dr. John Smith").
     */
    @NotBlank(message = "Name is required")
    private String name;

    /**
     * The user's email address. Must be unique across the system.
     * Serves as the primary login identifier.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    /**
     * The user's password in plain text. Will be BCrypt-encoded before storage.
     * Must be at least 6 characters long for security.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    /**
     * The role to assign to this user account.
     * Determines what endpoints and operations the user can access.
     */
    @NotNull(message = "Role is required")
    private Role role;
}
