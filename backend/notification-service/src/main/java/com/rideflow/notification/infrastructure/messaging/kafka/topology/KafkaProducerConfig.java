package com.rideflow.notification.infrastructure.messaging.kafka.topology;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo-side producer used by {@link com.rideflow.notification.api.rest.v1.DemoController}
 * to publish a synthetic ride event into Kafka so the full
 * consume → fan-out → broadcast pipeline can be exercised end-to-end.
 *
 * <p>This is intentionally a separate template from the DLQ template wired in
 * {@link KafkaConsumerConfig}: DLQ uses byte-array passthrough; demo uses
 * String serialization for human-readable Kafka UI inspection.
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, String> demoProducerFactory(
            KafkaProperties props,
            @Value("${spring.application.name}") String appName) {
        Map<String, Object> cfg = new HashMap<>(props.buildProducerProperties(null));
        cfg.put(ProducerConfig.CLIENT_ID_CONFIG,            appName + "-demo-producer");
        cfg.put(ProducerConfig.ACKS_CONFIG,                 "all");
        cfg.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,   true);
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
        cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(cfg);
    }

    @Bean
    public KafkaTemplate<String, String> demoKafkaTemplate(
            ProducerFactory<String, String> demoProducerFactory) {
        return new KafkaTemplate<>(demoProducerFactory);
    }
}
