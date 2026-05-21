package com.hospital.appointment.config;

import com.hospital.appointment.kafka.event.AppointmentEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer configuration for the Appointment Service.
 *
 * Configures the ProducerFactory and KafkaTemplate with JSON serialization
 * and type mappings so that downstream consumers can deserialize events
 * using their own class definitions.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Creates a ProducerFactory configured for String keys and AppointmentEvent values.
     * Uses JsonSerializer with explicit type mapping header so consumers can
     * map the "appointmentEvent" type to their own event class.
     *
     * @return configured ProducerFactory
     */
    @Bean
    public ProducerFactory<String, AppointmentEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Type mapping header: tells consumers how to map the JSON type
        configProps.put(JsonSerializer.TYPE_MAPPINGS,
                "appointmentEvent:com.hospital.appointment.kafka.event.AppointmentEvent");

        // Ensure reliable delivery
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Creates a KafkaTemplate wrapping the ProducerFactory.
     * This template is injected into the AppointmentEventProducer
     * for all event publishing operations.
     *
     * @return configured KafkaTemplate
     */
    @Bean
    public KafkaTemplate<String, AppointmentEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
