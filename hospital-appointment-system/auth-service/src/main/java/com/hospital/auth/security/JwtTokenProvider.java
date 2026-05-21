package com.hospital.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * ============================================================================
 * JWT Token Provider
 * ============================================================================
 * Centralized utility for all JWT operations in the auth-service. Handles:
 *
 * 1. Token Generation - Creates signed JWTs containing user email and role
 * 2. Token Parsing    - Extracts claims (email, role) from valid tokens
 * 3. Token Validation - Verifies signature, expiration, and structure
 *
 * Security details:
 * - Uses HMAC-SHA512 algorithm for token signing
 * - The signing key is derived from the configured secret using Keys.hmacShaKeyFor()
 * - Token expiration is configurable (default: 24 hours)
 *
 * The same secret must be shared across all microservices that need to
 * validate tokens issued by this service.
 * ============================================================================
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /**
     * The secret key string used to generate the HMAC signing key.
     * Injected from application.yml (jwt.secret).
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Token validity duration in milliseconds.
     * Injected from application.yml (jwt.expiration). Default: 86400000 (24h).
     */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * The cryptographic signing key derived from the secret string.
     * Initialized once at startup via @PostConstruct.
     */
    private Key signingKey;

    /**
     * Initializes the HMAC signing key from the configured secret.
     * Called automatically after dependency injection is complete.
     */
    @PostConstruct
    public void init() {
        // Convert the secret string to bytes and create an HMAC-SHA key
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        log.info("JWT Token Provider initialized with expiration: {} ms", jwtExpiration);
    }

    /**
     * Generates a new JWT token for an authenticated user.
     *
     * The token contains:
     * - Subject (sub): user's email address
     * - Role claim: user's role (ADMIN, DOCTOR, PATIENT)
     * - Issued at (iat): current timestamp
     * - Expiration (exp): current time + configured expiration duration
     *
     * @param email the user's email address (used as the token subject)
     * @param role  the user's role as a string
     * @return a signed JWT token string
     */
    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        String token = Jwts.builder()
                .setSubject(email)                          // Primary identifier
                .claim("role", role)                        // Custom claim for authorization
                .setIssuedAt(now)                           // Token creation timestamp
                .setExpiration(expiryDate)                  // Token expiry timestamp
                .signWith(signingKey, SignatureAlgorithm.HS512)  // Sign with HMAC-SHA512
                .compact();

        log.debug("Generated JWT token for user: {}, role: {}, expires: {}", email, role, expiryDate);
        return token;
    }

    /**
     * Extracts the email address (subject) from a JWT token.
     *
     * @param token the JWT token string
     * @return the email address encoded in the token's subject claim
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    /**
     * Extracts the role from a JWT token's custom claims.
     *
     * @param token the JWT token string
     * @return the role string encoded in the token
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Validates a JWT token by attempting to parse and verify it.
     *
     * Checks performed:
     * - Signature verification (using the signing key)
     * - Expiration check (token must not be expired)
     * - Structural integrity (must be a well-formed JWT)
     *
     * @param token the JWT token string to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            // Attempt to parse the token; this will throw if invalid
            Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Malformed JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Parses the JWT token and extracts the claims body.
     * This is an internal utility method used by getEmailFromToken and getRoleFromToken.
     *
     * @param token the JWT token string
     * @return the Claims object containing all token claims
     */
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
