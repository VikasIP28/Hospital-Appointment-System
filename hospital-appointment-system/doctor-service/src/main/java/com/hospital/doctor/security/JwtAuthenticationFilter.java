package com.hospital.doctor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================================
 * JWT Authentication Filter
 * ============================================================================
 * Intercepts every incoming HTTP request to validate JWT tokens.
 * This filter extends OncePerRequestFilter to guarantee single execution
 * per request.
 *
 * IMPORTANT: Unlike auth-service, doctor-service does NOT have a
 * UserDetailsService or UserRepository. Authentication is created
 * directly from JWT claims (email + role) without database lookups.
 *
 * Flow:
 * 1. Extract Bearer token from the Authorization header
 * 2. Validate the token using JwtTokenProvider
 * 3. Extract email and role from token claims
 * 4. Create a UsernamePasswordAuthenticationToken with role-based authority
 * 5. Set the authentication in SecurityContext for downstream access
 * ============================================================================
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT token utility for validation and claim extraction */
    private final JwtTokenProvider tokenProvider;

    /**
     * Process each request to extract and validate JWT token.
     * If a valid token is found, sets up the SecurityContext with
     * the authenticated user's information.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Step 1: Extract JWT token from the Authorization header
            String jwt = getJwtFromRequest(request);

            // Step 2: Validate the token and set up authentication
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {

                // Step 3: Extract claims directly from the JWT token
                String email = tokenProvider.getEmailFromToken(jwt);

                log.debug("JWT validated for user: {}, role: {}", email, tokenProvider.getRoleFromToken(jwt));

                // Step 4: Create authority from role claim
                // The role in JWT is stored as "ADMIN", "DOCTOR", "PATIENT"
                 String role = tokenProvider.getRoleFromToken(jwt);
                if (role != null && !role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }

                // Build granted authorities from the role claim
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority(role)
                );

                // Step 5: Create authentication token
                // No UserDetails needed - we create auth directly from JWT claims
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);

                // Attach request details (IP address, session ID, etc.)
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Step 6: Set authentication in SecurityContext
                // This makes the user's identity available to controllers via @PreAuthorize
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authentication set for user: {} with authorities: {}", email, authorities);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        // Continue the filter chain regardless of authentication result
        filterChain.doFilter(request, response);
    }

    /**
     * Extract the JWT token from the HTTP request's Authorization header.
     * Expects the format: "Bearer <token>"
     *
     * @param request the incoming HTTP request
     * @return the JWT token string, or null if not present/invalid format
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        // Check if the header exists and starts with "Bearer "
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix (7 characters)
        }

        return null;
    }
}
