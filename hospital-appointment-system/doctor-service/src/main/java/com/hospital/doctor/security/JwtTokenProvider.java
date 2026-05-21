package com.hospital.doctor.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * ============================================================================
 * JWT Token Provider - Token Validation Utility
 * ============================================================================
 * Provides JWT token validation and claim extraction for the doctor-service.
 * This is the SAME implementation as used in auth-service to ensure
 * cross-service token compatibility.
 *
 * IMPORTANT: This service only VALIDATES tokens - it does NOT generate them.
 * Token generation is handled exclusively by the auth-service.
 *
 * The JWT secret and expiration must match the auth-service configuration
 * to allow tokens issued by auth-service to be validated here.
 * ============================================================================
 */
@Component
@Slf4j
public class JwtTokenProvider {

    /** JWT signing secret - must match auth-service's secret exactly */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /** JWT token expiration time in milliseconds */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Create a cryptographic signing key from the JWT secret string.
     * Uses HMAC-SHA algorithm for symmetric key signing.
     *
     * @return the signing Key derived from the secret
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Extract the email (subject) from a JWT token.
     * The email is stored as the "sub" (subject) claim during token creation.
     *
     * @param token the JWT token string
     * @return the email address stored in the token
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * Extract the role from a JWT token.
     * The role is stored as a custom "role" claim during token creation
     * (e.g., "ADMIN", "DOCTOR", "PATIENT").
     *
     * @param token the JWT token string
     * @return the role stored in the token
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("role", String.class);
    }

    /**
     * Validate a JWT token for correctness, expiration, and integrity.
     * Checks:
     * 1. The token signature matches our secret key
     * 2. The token has not expired
     * 3. The token format is valid (well-formed JWT)
     * 4. The token is a supported JWT type
     * 5. The claims string is not empty
     *
     * @param token the JWT token string to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }
}
