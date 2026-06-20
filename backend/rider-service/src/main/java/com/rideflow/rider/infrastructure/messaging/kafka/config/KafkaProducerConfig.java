package com.rideflow.rider.infrastructure.messaging.kafka.config;

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
 * Producer-side wiring for the outbox relay. Explicit (not Boot-autoconfigured)
 * because the consumer side also defines a byte-array DLQ producer factory;
 * without this {@code @Primary} String template, the relay could bind to the DLQ
 * factory and serialize its JSON payloads with the wrong serializer.
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
