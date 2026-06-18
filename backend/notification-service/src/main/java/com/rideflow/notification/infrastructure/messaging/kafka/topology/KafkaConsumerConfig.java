package com.rideflow.notification.infrastructure.messaging.kafka.topology;

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
 * Consumer-side Kafka configuration. Mirrors {@code location-service}'s
 * topology — backoff + DLQ semantics are identical across services for
 * operational consistency (Rule 6).
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ProducerFactory<Object, Object> dlqProducerFactory(
            KafkaProperties props,
            @Value("${spring.application.name}") String appName) {
        Map<String, Object> cfg = new HashMap<>(props.buildProducerProperties(null));
        cfg.put(ProducerConfig.CLIENT_ID_CONFIG,            appName + "-dlq-producer");
        cfg.put(ProducerConfig.ACKS_CONFIG,                 "all");
        cfg.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,   true);
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
        cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        return new DefaultKafkaProducerFactory<>(cfg);
    }

    @Bean
    public KafkaOperations<Object, Object> dlqKafkaTemplate(ProducerFactory<Object, Object> factory) {
        return new KafkaTemplate<>(factory);
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> dlqTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                dlqTemplate,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(
                        Topics.dlqOf(record.topic()),
                        -1
                ));

        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(100L);
        backOff.setMultiplier(3.0);
        backOff.setMaxInterval(3_000L);
        backOff.setMaxElapsedTime(3_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(
                DeserializationException.class,
                IllegalArgumentException.class
        );
        return handler;
    }

    // Build the factory from Boot's auto-configured Configurer + ConsumerFactory
    // (group id, offsets, manual ack, concurrency, ErrorHandlingDeserializer all
    // come from application.yml), then attach the error handler. We must NOT
    // inject a ConcurrentKafkaListenerContainerFactory parameter here: declaring
    // this @Bean with the name `kafkaListenerContainerFactory` suppresses Boot's
    // auto-configured bean of the same name, so the parameter would resolve to
    // this very bean — an unresolvable self-referential cycle.
    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
