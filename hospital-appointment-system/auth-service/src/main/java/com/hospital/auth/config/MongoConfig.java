package com.hospital.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * ============================================================================
 * MongoDB Configuration
 * ============================================================================
 * Enables MongoDB auditing features which allow automatic population of
 * audit-related fields annotated with @CreatedDate, @LastModifiedDate,
 * @CreatedBy, and @LastModifiedBy.
 *
 * While the User entity currently uses manual timestamp management
 * (setting createdAt in the service layer), this configuration enables
 * the auditing infrastructure for future entities that may use Spring
 * Data's auditing annotations.
 * ============================================================================
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {

    // MongoDB auditing is enabled via the @EnableMongoAuditing annotation.
    // No additional bean definitions are required for basic auditing support.
    // Spring Boot auto-configures the MongoTemplate and MongoClient beans
    // based on the application.yml settings.
}
