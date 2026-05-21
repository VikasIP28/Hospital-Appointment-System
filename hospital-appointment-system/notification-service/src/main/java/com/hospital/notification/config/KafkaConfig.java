package com.hospital.notification.config;

import com.hospital.notification.kafka.event.AppointmentEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration for the notification-service.
 *
 * Configures the ConsumerFactory and KafkaListenerContainerFactory to deserialize
 * incoming Kafka messages as AppointmentEvent objects using Spring Kafka's JsonDeserializer.
 *
 * Key settings:
 * - Trusted packages set to '*' to allow deserialization from any package
 * - Type mapping configured to map 'appointmentEvent' type header to AppointmentEvent class
 * - Consumer group 'notification-group' ensures each notification-service instance
 *   receives every message (when running single instance) or load-balances across instances
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * Creates the Kafka ConsumerFactory configured for AppointmentEvent deserialization.
     *
     * The JsonDeserializer is configured to:
     * 1. Trust all packages ('*') so events from any service can be deserialized
     * 2. Use AppointmentEvent as the default deserialization target type
     * 3. Not use the type info headers if missing (falls back to default type)
     */
    @Bean
    public ConsumerFactory<String, AppointmentEvent> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Configure the JSON deserializer for AppointmentEvent (ignore type headers)
        JsonDeserializer<AppointmentEvent> jsonDeserializer = new JsonDeserializer<>(AppointmentEvent.class, false);
        jsonDeserializer.setRemoveTypeHeaders(false);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeMapperForKey(false);

        return new DefaultKafkaConsumerFactory<>(
                configProps,
                new StringDeserializer(),
                jsonDeserializer
        );
    }

    /**
     * Creates the KafkaListenerContainerFactory used by @KafkaListener annotations.
     * This factory creates concurrent message listener containers that poll Kafka
     * and dispatch messages to the annotated handler methods.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AppointmentEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AppointmentEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
