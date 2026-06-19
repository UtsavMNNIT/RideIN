package com.rideflow.trip.infrastructure.messaging.kafka.producer;

import com.rideflow.trip.infrastructure.persistence.jpa.entity.OutboxEvent;
import com.rideflow.trip.infrastructure.persistence.jpa.repository.OutboxJpaRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Drains the transactional outbox ({@code trip.outbox}) to Kafka. At-least-once:
 * a row is sent then marked published in the same transaction; a crash between
 * re-sends next tick (consumers dedupe on {@code eventId}). Fail-fast per tick,
 * oldest-first, preserving per-ride order.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxJpaRepository           outbox;
    private final KafkaTemplate<String, String> kafka;
    private final int                           batchSize;
    private final int                           maxAttempts;
    private final long                          sendTimeoutMs;
    private final Counter                       publishedCounter;
    private final Counter                       failedCounter;

    public OutboxRelay(OutboxJpaRepository outbox,
                       KafkaTemplate<String, String> kafka,
                       @Value("${rideflow.trip.outbox.batch-size}")   int batchSize,
                       @Value("${rideflow.trip.outbox.max-attempts}") int maxAttempts,
                       @Value("${rideflow.trip.outbox.send-timeout-ms:5000}") long sendTimeoutMs,
                       MeterRegistry registry) {
        this.outbox           = outbox;
        this.kafka            = kafka;
        this.batchSize        = batchSize;
        this.maxAttempts      = maxAttempts;
        this.sendTimeoutMs    = sendTimeoutMs;
        this.publishedCounter = registry.counter("trip.outbox.published");
        this.failedCounter    = registry.counter("trip.outbox.send_failed");
    }

    @Scheduled(fixedDelayString = "${rideflow.trip.outbox.poll-interval-ms}")
    @Transactional
    public void drain() {
        List<OutboxEvent> batch = outbox.findPublishable(maxAttempts, PageRequest.of(0, batchSize));
        if (batch.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        int published = 0;
        for (OutboxEvent event : batch) {
            try {
                kafka.send(event.getTopic(), event.getPartitionKey(), event.getPayload())
                     .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
                event.markPublished(now);
                published++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                event.recordFailure(truncate(e.getMessage()));
                failedCounter.increment();
                log.warn("Outbox send failed eventId={} topic={} attempt={}; retry next tick",
                        event.getEventId(), event.getTopic(), event.getAttemptCount(), e);
                break;
            }
        }

        if (published > 0) {
            publishedCounter.increment(published);
            log.debug("Outbox relay published {} event(s)", published);
        }
    }

    private static String truncate(String s) {
        if (s == null) return "unknown";
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }
}
