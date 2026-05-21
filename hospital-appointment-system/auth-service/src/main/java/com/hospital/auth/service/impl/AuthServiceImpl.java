package com.hospital.auth.service.impl;

import com.hospital.auth.dto.AuthResponse;
import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.RegisterRequest;
import com.hospital.auth.entity.User;
import com.hospital.auth.exception.AuthException;
import com.hospital.auth.exception.UserAlreadyExistsException;
import com.hospital.auth.repository.UserRepository;
import com.hospital.auth.security.JwtTokenProvider;
import com.hospital.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * Auth Service Implementation
 * ============================================================================
 * Production implementation of the AuthService interface. Handles all
 * authentication business logic including:
 *
 * 1. User Registration:
 *    - Checks for duplicate email addresses
 *    - Encodes the password using BCrypt
 *    - Persists the user to MongoDB
 *    - Generates a JWT token for immediate use after registration
 *
 * 2. User Login:
 *    - Looks up the user by email
 *    - Verifies the password against the stored BCrypt hash
 *    - Generates a JWT token upon successful authentication
 *
 * 3. Token Validation:
 *    - Delegates to JwtTokenProvider for token verification
 *    - Extracts email and role claims from valid tokens
 *
 * All operations are logged at appropriate levels for monitoring and debugging.
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Registers a new user account.
     *
     * Flow:
     * 1. Check if the email is already in use (throw 409 if duplicate)
     * 2. Encode the raw password with BCrypt
     * 3. Build and save the User entity to MongoDB
     * 4. Generate a JWT token for the new user
     * 5. Return an AuthResponse with the token and user details
     *
     * @param registerRequest the registration data
     * @return AuthResponse with JWT token and user information
     * @throws UserAlreadyExistsException if the email is already registered
     */
    @Override
    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("Processing registration request for email: {}", registerRequest.getEmail());

        // Step 1: Check for duplicate email
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Registration failed - email already exists: {}", registerRequest.getEmail());
            throw new UserAlreadyExistsException(
                    "User with email " + registerRequest.getEmail() + " already exists"
            );
        }

        // Step 2: Build the User entity with encoded password and current timestamp
        User user = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(registerRequest.getRole())
                .createdAt(LocalDateTime.now())
                .build();

        // Step 3: Persist the user to MongoDB
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}, role: {}, id: {}",
                savedUser.getEmail(), savedUser.getRole(), savedUser.getId());

        // Step 4: Generate JWT token for immediate use
        String token = jwtTokenProvider.generateToken(
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        // Step 5: Build and return the response
        return AuthResponse.builder()
                .token(token)
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .name(savedUser.getName())
                .build();
    }

    /**
     * Authenticates a user with email and password credentials.
     *
     * Flow:
     * 1. Look up the user by email (throw 401 if not found)
     * 2. Verify the password against the stored BCrypt hash (throw 401 if mismatch)
     * 3. Generate a JWT token
     * 4. Return an AuthResponse with the token and user details
     *
     * @param loginRequest the login credentials
     * @return AuthResponse with JWT token and user information
     * @throws AuthException if the credentials are invalid
     */
    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Processing login request for email: {}", loginRequest.getEmail());

        // Step 1: Find the user by email
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found: {}", loginRequest.getEmail());
                    return new AuthException("Invalid email or password");
                });

        // Step 2: Verify the password matches the stored hash
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("Login failed - password mismatch for user: {}", loginRequest.getEmail());
            throw new AuthException("Invalid email or password");
        }

        // Step 3: Generate JWT token
        String token = jwtTokenProvider.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        log.info("User logged in successfully: {}, role: {}", user.getEmail(), user.getRole());

        // Step 4: Build and return the response
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .name(user.getName())
                .build();
    }

    /**
     * Validates a JWT token and extracts user information from it.
     *
     * This endpoint is typically called by other microservices (via the
     * API Gateway) to verify the authenticity of a token and get the
     * user's email and role for authorization decisions.
     *
     * @param token the JWT token string to validate
     * @return a Map containing:
     *         - "valid": boolean indicating if the token is valid
     *         - "email": the user's email (null if invalid)
     *         - "role": the user's role (null if invalid)
     */
    @Override
    public Map<String, Object> validateToken(String token) {
        log.debug("Validating JWT token");

        Map<String, Object> response = new HashMap<>();

        // Delegate validation to JwtTokenProvider
        boolean isValid = jwtTokenProvider.validateToken(token);
        response.put("valid", isValid);

        if (isValid) {
            // Extract claims only if the token is valid
            String email = jwtTokenProvider.getEmailFromToken(token);
            String role = jwtTokenProvider.getRoleFromToken(token);
            response.put("email", email);
            response.put("role", role);
            log.debug("Token is valid for user: {}, role: {}", email, role);
        } else {
            response.put("email", null);
            response.put("role", null);
            log.warn("Token validation failed");
        }

        return response;
    }
}
