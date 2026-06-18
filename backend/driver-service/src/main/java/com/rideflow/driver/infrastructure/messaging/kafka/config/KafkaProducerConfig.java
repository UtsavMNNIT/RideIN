package com.rideflow.driver.infrastructure.messaging.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.common.events.EventEnvelope;
import com.rideflow.driver.infrastructure.messaging.kafka.serde.JsonEventSerializer;

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
 * Kafka producer configuration for driver-service.
 *
 * Tuned for the dominant workload — driver location telemetry @ ~5K msg/s,
 * key=driverId for partition ordering, lossy-acceptable under broker outage.
 *
 * <p>Rationale for each non-default property is inline. Anything not set here
 * inherits from {@code spring.kafka.*} in application.yml.
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, EventEnvelope<?>> envelopeProducerFactory(
            KafkaProperties springKafkaProps,
            ObjectMapper objectMapper,
            @Value("${spring.application.name}") String appName) {

        Map<String, Object> cfg = new HashMap<>(springKafkaProps.buildProducerProperties(null));

        // ---------- Identity ----------
        cfg.put(ProducerConfig.CLIENT_ID_CONFIG, appName + "-producer");

        // ---------- Durability + ordering ----------
        // acks=all  : leader waits for ISR commit before ack
        // idempotence: broker dedups producer retries (no duplicate writes)
        // max.in.flight=5 : ordering preserved BECAUSE idempotence is on
        cfg.put(ProducerConfig.ACKS_CONFIG, "all");
        cfg.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        cfg.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // ---------- Throughput ----------
        // linger.ms=10 + batch=64KB → small wait for accumulation, 3-5x throughput.
        // lz4 matches broker default, fast and cheap.
        cfg.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        cfg.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        cfg.put(ProducerConfig.BATCH_SIZE_CONFIG, 64 * 1024);
        cfg.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 64L * 1024 * 1024);

        // ---------- Retry / delivery deadline ----------
        // retries=MAX_INT, bounded by delivery.timeout.ms (30s).
        // max.block.ms caps send() blocking when buffer is saturated → back-pressure ceiling.
        cfg.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        cfg.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);
        cfg.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30_000);
        cfg.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10_000);
        cfg.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5_000);

        // ---------- Serialization ----------
        // Key = String (driverId), value = our JSON envelope serializer.
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        DefaultKafkaProducerFactory<String, EventEnvelope<?>> factory =
                new DefaultKafkaProducerFactory<>(cfg);
        factory.setValueSerializer(new JsonEventSerializer<>(objectMapper));
        return factory;
    }

    @Bean
    public KafkaTemplate<String, EventEnvelope<?>> envelopeKafkaTemplate(
            ProducerFactory<String, EventEnvelope<?>> factory) {
        KafkaTemplate<String, EventEnvelope<?>> template = new KafkaTemplate<>(factory);
        // Micrometer-backed tracing + metrics (Phase 9 dashboards plug in).
        template.setObservationEnabled(true);
        return template;
    }
}
