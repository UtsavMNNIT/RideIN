package com.rideflow.matching.infrastructure.messaging.kafka.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Producer-side wiring for the outbox relay. The relay ships pre-serialized
 * {@code EventEnvelope} JSON strings, so both key and value are plain strings.
 *
 * <p>{@code acks=all} + idempotence come from {@code spring.kafka.producer.*}
 * in {@code application.yml}; we only pin the serializers (Boot's autoconfig
 * leaves them unset because we also run an {@code ErrorHandlingDeserializer}
 * consumer in this app).
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, String> stringProducerFactory(KafkaProperties props) {
        Map<String, Object> cfg = new HashMap<>(props.buildProducerProperties(null));
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
        cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(cfg);
    }

    @Bean
    @Primary
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> stringProducerFactory) {
        return new KafkaTemplate<>(stringProducerFactory);
    }
}
