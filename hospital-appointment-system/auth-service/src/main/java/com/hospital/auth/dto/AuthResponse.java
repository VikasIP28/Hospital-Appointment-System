package com.hospital.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ============================================================================
 * Auth Response DTO
 * ============================================================================
 * Data Transfer Object returned to the client after a successful login
 * or registration. Contains the JWT token along with essential user
 * information that the client can use without making additional API calls.
 *
 * The token should be included in the Authorization header of subsequent
 * requests as: "Bearer <token>"
 * ============================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * The JWT token for authenticating subsequent requests.
     * Contains encoded claims: email (subject), role, and expiration time.
     */
    private String token;

    /**
     * The authenticated user's email address.
     */
    private String email;

    /**
     * The authenticated user's role (ADMIN, DOCTOR, or PATIENT).
     * Returned as a string for client-side convenience.
     */
    private String role;

    /**
     * The authenticated user's display name.
     */
    private String name;
}
