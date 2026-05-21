package com.hospital.auth.security;

import com.hospital.auth.entity.User;
import com.hospital.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

/**
 * ============================================================================
 * Custom UserDetailsService Implementation
 * ============================================================================
 * This service bridges the gap between our MongoDB User entity and Spring
 * Security's UserDetails interface. It is used by:
 *
 * 1. The JwtAuthenticationFilter - to load user details during token validation
 * 2. The AuthenticationManager - to authenticate users during login
 *
 * Role mapping convention:
 * - Our Role enum: ADMIN, DOCTOR, PATIENT
 * - Spring Security expects: ROLE_ADMIN, ROLE_DOCTOR, ROLE_PATIENT
 *
 * The "ROLE_" prefix is added automatically here so that @PreAuthorize
 * annotations with hasRole('ADMIN') work correctly (Spring Security
 * internally prefixes "ROLE_" when checking hasRole).
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by their email address (used as the "username" in our system).
     *
     * @param email the user's email address
     * @return a Spring Security UserDetails object populated with user data
     * @throws UsernameNotFoundException if no user exists with the given email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user details for email: {}", email);

        // Fetch the user from MongoDB by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        log.debug("User found: {}, role: {}", user.getEmail(), user.getRole());

        // Map our Role enum to Spring Security's GrantedAuthority with ROLE_ prefix
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        // Build and return a Spring Security User object
        // Parameters: username, password, enabled, accountNonExpired,
        //             credentialsNonExpired, accountNonLocked, authorities
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                true,    // enabled
                true,    // accountNonExpired
                true,    // credentialsNonExpired
                true,    // accountNonLocked
                authorities
        );
    }
}
