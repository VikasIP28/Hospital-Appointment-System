package com.hospital.doctor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * ============================================================================
 * MongoDB Configuration
 * ============================================================================
 * Enables MongoDB auditing support for automatic population of
 * audit-related fields in document entities.
 *
 * With @EnableMongoAuditing enabled, the following annotations will work:
 * - @CreatedDate: Automatically sets the creation timestamp
 * - @LastModifiedDate: Automatically sets the modification timestamp
 * - @CreatedBy: Automatically sets the creator (requires AuditorAware bean)
 * - @LastModifiedBy: Automatically sets the modifier
 *
 * In the Doctor entity, @CreatedDate is used on the 'createdAt' field
 * to automatically record when a doctor profile was first created.
 * ============================================================================
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
    // MongoDB auditing is enabled via the @EnableMongoAuditing annotation.
    // No additional beans are needed for basic @CreatedDate functionality.
}
