package com.hospital.auth.repository;

import com.hospital.auth.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ============================================================================
 * User Repository - MongoDB Data Access Layer
 * ============================================================================
 * Spring Data MongoDB repository for the User entity. Provides standard
 * CRUD operations inherited from MongoRepository plus custom query methods
 * for authentication workflows.
 *
 * Spring Data automatically generates the implementation at runtime based
 * on the method naming convention (derived queries).
 * ============================================================================
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Finds a user by their email address.
     * Used during login to retrieve credentials and role information.
     *
     * @param email the email address to search for
     * @return an Optional containing the User if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given email address already exists.
     * Used during registration to prevent duplicate accounts.
     *
     * @param email the email address to check
     * @return true if a user with this email exists, false otherwise
     */
    boolean existsByEmail(String email);
}
