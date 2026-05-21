package com.hospital.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
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
 * JWT Authentication Filter
 * ==========================
 * A servlet filter that runs once per request to authenticate users via JWT.
 *
 * How it works:
 * 1. Extracts the JWT token from the "Authorization: Bearer <token>" header
 * 2. Validates the token using JwtTokenProvider
 * 3. If valid, extracts the user's email and role from the token claims
 * 4. Creates a Spring Security Authentication object with the user's details
 * 5. Sets the authentication in the SecurityContext for downstream processing
 *
 * This filter does NOT use a UserDetailsService or database lookup.
 * All necessary user information (email, role) is embedded in the JWT token,
 * making authentication stateless and suitable for a gateway service.
 *
 * The role extracted from the JWT is prefixed with "ROLE_" to match
 * Spring Security's convention for hasRole() checks.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    // Standard prefix for Bearer tokens in the Authorization header
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // Step 1: Extract JWT from the Authorization header
            String jwt = extractJwtFromRequest(request);

            // Step 2: Validate and authenticate if token is present
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // Step 3: Extract user details from token claims
                String email = jwtTokenProvider.getEmailFromToken(jwt);

                log.debug("Authenticated request from user: {} with role: {} for URI: {}",
                        email, jwtTokenProvider.getRoleFromToken(jwt), request.getRequestURI());

                // Step 4: Build granted authorities list with ROLE_ prefix
                String role = jwtTokenProvider.getRoleFromToken(jwt);
                if (role != null && !role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }

                // Build granted authorities from the role claim
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority(role)
                );

                // Step 5: Create authentication token (no credentials needed since JWT is already validated)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);

                // Attach request details (remote address, session ID, etc.) to the authentication
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Step 6: Set authentication in the SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Log the error but don't block the request - let Spring Security handle unauthorized access
            log.error("Could not set user authentication in security context: {}", e.getMessage());
        }

        // Continue the filter chain regardless of authentication outcome
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the HTTP request's Authorization header.
     * Expects the header format: "Bearer <token>"
     *
     * @param request the incoming HTTP request
     * @return the JWT token string, or null if not present or malformed
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
