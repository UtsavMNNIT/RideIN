package com.rideflow.driver.infrastructure.messaging.kafka.producer;

import com.rideflow.common.events.EventEnvelope;
import com.rideflow.common.events.EventTypes;
import com.rideflow.common.events.Topics;
import com.rideflow.driver.domain.event.DomainEventPublisher;
import com.rideflow.driver.domain.event.DriverLocationUpdated;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Tracer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka adapter for {@link DomainEventPublisher}.
 *
 * <h3>Design contract</h3>
 * <ul>
 *   <li><b>Non-blocking:</b> the HTTP request thread does not wait on the broker.
 *       send() returns a {@link CompletableFuture}; outcome is observed via callback.</li>
 *   <li><b>Lossy-acceptable for telemetry:</b> on producer-level retry exhaustion
 *       we drop the message and record a counter. Driver pings every 3-5s;
 *       losing one is invisible to the dispatch algorithm.</li>
 *   <li><b>Strict ordering per driver:</b> partition key = driverId.</li>
 *   <li><b>No application-level retry, no DLQ.</b> A DLQ at the producer is a code
 *       smell — if the broker is unreachable, another topic on the same broker
 *       can't save us. Drop + observe + alert (Prometheus rule in Phase 9).</li>
 * </ul>
 */
@Component
public class DriverLocationEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DriverLocationEventPublisher.class);

    private static final int    SCHEMA_VERSION = 1;
    private static final String SOURCE         = "driver-service";

    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;
    private final Tracer        tracer;
    private final Counter       publishedCounter;
    private final Counter       failedCounter;
    private final Counter       fatalCounter;
    private final Timer         publishTimer;

    public DriverLocationEventPublisher(
            KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate,
            Tracer tracer,
            MeterRegistry registry) {
        this.kafkaTemplate = kafkaTemplate;
        this.tracer        = tracer;

        this.publishedCounter = Counter.builder("rideflow.events.published")
                .description("Successfully published events")
                .tag("topic", Topics.DRIVER_LOCATION_UPDATED)
                .register(registry);
        this.failedCounter = Counter.builder("rideflow.events.failed")
                .description("Events dropped after producer-level retries exhausted")
                .tag("topic", Topics.DRIVER_LOCATION_UPDATED)
                .register(registry);
        this.fatalCounter = Counter.builder("rideflow.events.fatal")
                .description("Non-retriable producer errors (config/auth bug)")
                .tag("topic", Topics.DRIVER_LOCATION_UPDATED)
                .register(registry);
        this.publishTimer = Timer.builder("rideflow.events.publish.duration")
                .description("End-to-end publish latency observed by the application")
                .tag("topic", Topics.DRIVER_LOCATION_UPDATED)
                .register(registry);
    }

    @Override
    public void publishLocationUpdate(DriverLocationUpdated payload) {
        String traceId = currentTraceId();

        EventEnvelope<DriverLocationUpdated> envelope = EventEnvelope.of(
                EventTypes.DRIVER_LOCATION_UPDATED,
                SCHEMA_VERSION,
                SOURCE,
                traceId,
                payload);

        ProducerRecord<String, EventEnvelope<?>> record = new ProducerRecord<>(
                Topics.DRIVER_LOCATION_UPDATED,
                payload.driverId().toString(),     // partition key → per-driver ordering
                envelope);

        Timer.Sample sample = Timer.start();

        CompletableFuture<SendResult<String, EventEnvelope<?>>> future =
                kafkaTemplate.send(record);

        future.whenComplete((result, ex) -> {
            sample.stop(publishTimer);

            if (ex == null) {
                publishedCounter.increment();
                if (log.isDebugEnabled()) {
                    log.debug("Published DriverLocationUpdated driverId={} partition={} offset={}",
                            payload.driverId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
                return;
            }

            // -------- failure paths --------
            Throwable cause = unwrap(ex);

            if (cause instanceof SerializationException
                    || cause instanceof UnknownTopicOrPartitionException
                    || cause instanceof AuthorizationException
                    || cause instanceof AuthenticationException) {
                // Non-retriable: config bug, schema bug, or auth misconfig.
                // Alert-worthy. Increment a SEPARATE counter so Phase 9 can page on it.
                fatalCounter.increment();
                log.error("FATAL producer error for DriverLocationUpdated driverId={} eventId={}: {}",
                        payload.driverId(), envelope.eventId(), cause.getClass().getSimpleName(), cause);
                return;
            }

            // Transient: broker down, buffer full, delivery.timeout exceeded.
            // Producer already retried up to delivery.timeout.ms; we drop.
            failedCounter.increment();
            log.warn("Dropped DriverLocationUpdated after retries — driverId={} eventId={} cause={}",
                    payload.driverId(), envelope.eventId(), cause.getClass().getSimpleName());
        });
    }

    private String currentTraceId() {
        if (tracer == null || tracer.currentSpan() == null) return null;
        return tracer.currentSpan().context().traceId();
    }

    private static Throwable unwrap(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }
}
