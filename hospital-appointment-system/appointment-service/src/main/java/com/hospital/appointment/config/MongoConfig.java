package com.hospital.appointment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB configuration for the Appointment Service.
 *
 * Enables MongoDB auditing so that @CreatedDate and @LastModifiedDate
 * annotations on entity fields are automatically populated when
 * documents are saved or updated.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
    // No additional beans required.
    // The @EnableMongoAuditing annotation activates the auditing
    // infrastructure which populates @CreatedDate fields automatically.
}
