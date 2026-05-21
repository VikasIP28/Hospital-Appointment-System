package com.hospital.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB configuration for the notification-service.
 *
 * Enables MongoDB auditing which automatically populates @CreatedDate
 * and @LastModifiedDate annotated fields in MongoDB document entities.
 * This is used by the Notification entity's createdAt field.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
    // MongoDB auditing is enabled via @EnableMongoAuditing annotation.
    // No additional bean definitions are needed - Spring Boot auto-configures
    // the MongoTemplate and MongoClient from application.yml properties.
}
