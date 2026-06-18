package com.rideflow.driver.infrastructure.messaging.kafka.serde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Jackson-backed serializer for {@code EventEnvelope<T>} (and anything else).
 *
 * Failures throw {@link SerializationException} so the producer's
 * {@code CompletableFuture<SendResult>} completes exceptionally and the
 * adapter records a metric / drops the message — instead of crashing the
 * HTTP request thread.
 *
 * <p>One serializer per producer is fine; Jackson's {@link ObjectMapper}
 * is thread-safe once configured.
 */
public final class JsonEventSerializer<T> implements Serializer<T> {

    private final ObjectMapper mapper;

    public JsonEventSerializer(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "ObjectMapper required");
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(data).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new SerializationException(
                    "Failed to serialize event for topic '" + topic + "'", e);
        }
    }
}
