package com.hospital.auth.security;

import com.hospital.auth.util.AppConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ============================================================================
 * JWT Authentication Filter
 * ============================================================================
 * A Spring Security filter that intercepts every incoming HTTP request to
 * check for a valid JWT token in the Authorization header.
 *
 * Processing flow:
 * 1. Extract the "Authorization" header from the request
 * 2. Check if it starts with "Bearer " prefix
 * 3. Extract the token string (strip the prefix)
 * 4. Validate the token using JwtTokenProvider
 * 5. If valid, load the UserDetails from the database
 * 6. Create an authentication token and set it in the SecurityContext
 * 7. Continue the filter chain
 *
 * This filter extends OncePerRequestFilter to guarantee a single execution
 * per request, even in complex filter chain configurations.
 * ============================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    /**
     * Core filter logic executed for every HTTP request.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filters in the chain
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Step 1: Extract the JWT token from the Authorization header
            String jwt = extractJwtFromRequest(request);

            // Step 2: Validate the token and set up the SecurityContext
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {

                // Extract the user's email from the token subject claim
                String email = jwtTokenProvider.getEmailFromToken(jwt);
                // The auth-service uses UserDetails to load authorities from the database
                // (CustomUserDetailsService will prepend "ROLE_" when converting Role enum to GrantedAuthority)
                // Load full user details from the database
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Create an authentication token with the user's details and authorities
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,             // Principal
                                null,                    // Credentials (not needed, already authenticated)
                                userDetails.getAuthorities()  // Granted authorities (roles)
                        );

                // Attach request details (IP, session ID, etc.) to the authentication
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Set the authentication in the SecurityContext so Spring Security
                // recognizes this request as authenticated
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("SecurityContext updated for user: {}, authorities: {}",
                        email, userDetails.getAuthorities());
            }
        } catch (Exception ex) {
            // Log the error but don't block the request — let Spring Security
            // handle unauthorized access via its entry points
            log.error("Could not set user authentication in security context: {}", ex.getMessage());
        }

        // Continue with the remaining filters in the chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the Authorization header.
     *
     * Expected format: "Bearer <token>"
     * If the header is missing or doesn't start with "Bearer ", returns null.
     *
     * @param request the HTTP request
     * @return the JWT token string, or null if not present
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AppConstants.JWT_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(AppConstants.JWT_PREFIX)) {
            return bearerToken.substring(AppConstants.JWT_PREFIX.length());
        }
        return null;
    }
}
