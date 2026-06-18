package com.rideflow.location.infrastructure.messaging.kafka.config;

import com.rideflow.common.events.Topics;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumer-side Kafka configuration.
 *
 * <ul>
 *   <li>Listener container factory inherits Spring Boot autoconfig (group id,
 *       offsets, manual ack, concurrency, ErrorHandlingDeserializer) — see
 *       {@code application.yml}. We attach the {@link DefaultErrorHandler}
 *       so retries + DLQ behaviour is explicit, not magical.</li>
 *   <li>A minimal {@link KafkaTemplate} is provisioned <b>only</b> for the
 *       {@link DeadLetterPublishingRecoverer}. It uses byte-array value
 *       serialization to forward poison messages verbatim, headers and all,
 *       without ever needing to deserialize them.</li>
 * </ul>
 */
@Configuration
public class KafkaConsumerConfig {

    // -------------------------------------------------------------------
    // DLQ publisher (byte-array value passthrough)
    // -------------------------------------------------------------------

    @Bean
    public ProducerFactory<Object, Object> dlqProducerFactory(
            KafkaProperties props,
            @Value("${spring.application.name}") String appName) {
        Map<String, Object> cfg = new HashMap<>(props.buildProducerProperties(null));
        cfg.put(ProducerConfig.CLIENT_ID_CONFIG, appName + "-dlq-producer");
        cfg.put(ProducerConfig.ACKS_CONFIG, "all");
        cfg.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
        cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        return new DefaultKafkaProducerFactory<>(cfg);
    }

    @Bean
    public KafkaOperations<Object, Object> dlqKafkaTemplate(ProducerFactory<Object, Object> factory) {
        return new KafkaTemplate<>(factory);
    }

    // -------------------------------------------------------------------
    // Error handler attached to the listener container
    // -------------------------------------------------------------------

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> dlqTemplate) {
        // Route every failed record to its topic's DLQ (same partition mirrored
        // when DLQ partition count matches; falls back to round-robin otherwise).
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                dlqTemplate,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(
                        Topics.dlqOf(record.topic()),
                        -1   // let producer choose by key hash on the DLQ
                ));

        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(100L);
        backOff.setMultiplier(3.0);
        backOff.setMaxInterval(3_000L);
        // 3 attempts total: initial + 2 retries; with multiplier 3.0 → ~0.4s of wait
        backOff.setMaxElapsedTime(3_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        // Poison messages — don't retry, go straight to DLQ.
        handler.addNotRetryableExceptions(
                DeserializationException.class,
                IllegalArgumentException.class
        );
        return handler;
    }

    // -------------------------------------------------------------------
    // Listener container factory — autoconfig provides the consumer factory;
    // we just attach the error handler.
    // -------------------------------------------------------------------

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler errorHandler) {
        // Build from Boot's autoconfig (Configurer + ConsumerFactory), then attach
        // the error handler. Injecting a ConcurrentKafkaListenerContainerFactory
        // here would resolve to this very bean (same name as Boot's auto-configured
        // one, which we override) — an unresolvable self-referential cycle.
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
