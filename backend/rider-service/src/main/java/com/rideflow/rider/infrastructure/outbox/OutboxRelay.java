package com.rideflow.rider.infrastructure.outbox;

import com.rideflow.rider.infrastructure.persistence.jpa.entity.OutboxEvent;
import com.rideflow.rider.infrastructure.persistence.jpa.repository.OutboxJpaRepository;

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
 * Drains the transactional outbox to Kafka.
 *
 * <p><b>Delivery semantics — at-least-once.</b> A row is sent, then marked
 * published in the same transaction. If the process dies after the send but
 * before the commit, the row is re-sent next cycle; consumers dedupe by the
 * envelope {@code eventId} (which is the outbox row id). We therefore never
 * lose an event, and duplicates are harmless.
 *
 * <p><b>Failure handling.</b> On the first send failure we stop the batch
 * <em>without</em> rolling back — rows already sent stay marked published (no
 * duplicate re-send), and the failed row plus everything after it is retried on
 * the next tick, preserving per-key order.
 *
 * <p><b>Multi-pod note.</b> Several pods may drain concurrently and double-send;
 * that's absorbed by consumer dedupe. If the redundant Kafka traffic ever
 * matters, claim rows with {@code SELECT ... FOR UPDATE SKIP LOCKED}.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxJpaRepository       outbox;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int                       batchSize;
    private final long                      sendTimeoutMs;
    private final Counter                   publishedCounter;

    public OutboxRelay(OutboxJpaRepository outbox,
                       KafkaTemplate<String, String> kafkaTemplate,
                       @Value("${rideflow.outbox.relay.batch-size}")     int batchSize,
                       @Value("${rideflow.outbox.relay.send-timeout-ms}") long sendTimeoutMs,
                       MeterRegistry registry) {
        this.outbox         = outbox;
        this.kafkaTemplate  = kafkaTemplate;
        this.batchSize      = batchSize;
        this.sendTimeoutMs  = sendTimeoutMs;
        this.publishedCounter = Counter.builder("rideflow.outbox.published")
                .description("Outbox events successfully published to Kafka")
                .register(registry);
    }

    @Scheduled(fixedDelayString = "${rideflow.outbox.relay.interval-ms}")
    @Transactional
    public void drain() {
        List<OutboxEvent> batch = outbox.findUnpublished(PageRequest.of(0, batchSize));
        if (batch.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        int published = 0;
        for (OutboxEvent event : batch) {
            try {
                kafkaTemplate
                        .send(event.getTopic(), event.getPartitionKey(), event.getPayload())
                        .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
                event.markPublished(now);   // managed entity — flushed on commit
                published++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Stop here; do NOT rethrow (rolling back would re-send the rows we
                // already published this batch). Retry from this row next tick.
                log.warn("Outbox send failed for eventId={} topic={}; will retry next tick",
                        event.getId(), event.getTopic(), e);
                break;
            }
        }

        if (published > 0) {
            publishedCounter.increment(published);
            log.debug("Outbox relay published {} event(s)", published);
        }
    }
}
