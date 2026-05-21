package com.hospital.notification.security;

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
 * JWT Authentication Filter for the notification-service.
 *
 * This filter intercepts every incoming HTTP request and:
 * 1. Extracts the JWT token from the Authorization header (Bearer scheme)
 * 2. Validates the token signature and expiration using JwtTokenProvider
 * 3. Creates a Spring Security Authentication object directly from JWT claims
 *    (no UserDetailsService call needed - we trust the auth-service's token)
 * 4. Sets the authentication in the SecurityContext for downstream access control
 *
 * This approach avoids a database lookup for every request, making it suitable
 * for microservices that only need to validate identity from the JWT.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extract JWT from the Authorization header
            String jwt = getJwtFromRequest(request);

            // Validate and process the token if present
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // Extract user details from the validated JWT claims
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                String role = jwtTokenProvider.getRoleFromToken(jwt);
                if (role != null && !role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }

                log.debug("JWT validated for user: {}, role: {}, URI: {}", username, role, request.getRequestURI());

                // Build granted authorities from the role claim
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority(role)
                );
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                // Attach request details for audit/logging purposes
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set the authentication in the security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.error("Could not set user authentication in security context: {}", e.getMessage());
        }

        // Continue the filter chain regardless of authentication result
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the request's Authorization header.
     * Expects the format: "Bearer <token>"
     *
     * @param request the HTTP request
     * @return the JWT token string, or null if not present/invalid format
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
