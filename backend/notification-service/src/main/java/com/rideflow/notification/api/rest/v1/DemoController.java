package com.rideflow.notification.api.rest.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideflow.common.events.EventEnvelope;
import com.rideflow.common.events.EventTypes;
import com.rideflow.common.events.Topics;
import com.rideflow.notification.application.usecase.PublishNotificationUseCase;
import com.rideflow.notification.domain.model.Notification;
import com.rideflow.notification.domain.model.NotificationType;
import com.rideflow.notification.domain.model.Role;

import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Demo / smoke-test endpoint — toggled by {@code rideflow.demo.enabled=true}.
 *
 * <p>Two paths, both visible to the connected WebSocket:
 * <ul>
 *   <li>{@code DIRECT} — bypasses Kafka, calls the use case in-process.
 *       Proves: persistence + Redis pub/sub + WebSocket fan-out.</li>
 *   <li>{@code KAFKA} — publishes a real {@code matching.RideAssigned}
 *       envelope onto Kafka. Proves: the full production pipeline
 *       (consumer → idempotency → use case → broadcast).</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/demo")
@ConditionalOnProperty(name = "rideflow.demo.enabled", havingValue = "true")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    private final PublishNotificationUseCase    useCase;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper                  json;
    private final String                        consumerGroup;
    private final String                        sourceLabel;

    public DemoController(PublishNotificationUseCase useCase,
                          KafkaTemplate<String, String> demoKafkaTemplate,
                          ObjectMapper objectMapper,
                          @Value("${spring.kafka.consumer.group-id}") String consumerGroup,
                          @Value("${spring.application.name}") String sourceLabel) {
        this.useCase       = useCase;
        this.kafka         = demoKafkaTemplate;
        this.json          = objectMapper;
        this.consumerGroup = consumerGroup;
        this.sourceLabel   = sourceLabel;
    }

    @PostMapping("/notify")
    public ResponseEntity<Map<String, Object>> notify(@RequestBody NotifyRequest req) {
        UUID rideId  = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        if (req.trigger() == Trigger.DIRECT) {
            handleDirect(req, rideId, eventId);
        } else {
            handleKafka(req, rideId, eventId);
        }

        return ResponseEntity.ok(Map.of(
                "trigger",  req.trigger().name(),
                "userId",   req.userId(),
                "rideId",   rideId,
                "eventId",  eventId
        ));
    }

    // -----------------------------------------------------------------------

    private void handleDirect(NotifyRequest req, UUID rideId, UUID eventId) {
        Notification n = Notification.create(
                req.userId(),
                req.role(),
                req.type(),
                rideId,
                Map.of(
                        "demo",    "true",
                        "rideId",  rideId.toString(),
                        "message", "Triggered via DIRECT path (no Kafka)"
                )
        );
        useCase.handle(eventId, consumerGroup + ":demo-direct", List.of(n));
        log.info("Demo (direct) eventId={} userId={} type={}", eventId, req.userId(), req.type());
    }

    private void handleKafka(NotifyRequest req, UUID rideId, UUID eventId) {
        // Build a payload that matches the matching.RideAssigned wire shape.
        // The consumer fans this into two Notifications (rider + driver) —
        // only the one matching `req.userId` will reach the connected WS.
        UUID otherParty = UUID.randomUUID();
        UUID riderId  = req.role() == Role.RIDER  ? req.userId() : otherParty;
        UUID driverId = req.role() == Role.DRIVER ? req.userId() : otherParty;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rideId",      rideId.toString());
        payload.put("riderId",     riderId.toString());
        payload.put("driverId",    driverId.toString());
        payload.put("vehicleType", "STANDARD");
        payload.put("pickupLat",   40.7580);
        payload.put("pickupLng",  -73.9855);
        payload.put("dropoffLat",  40.7484);
        payload.put("dropoffLng", -73.9857);
        payload.put("etaSeconds",  300);
        payload.put("assignedAt",  Instant.now().toString());

        EventEnvelope<Map<String, Object>> envelope = new EventEnvelope<>(
                eventId,
                EventTypes.RIDE_ASSIGNED,
                1,
                Instant.now(),
                eventId.toString(),
                sourceLabel,
                payload
        );

        String body;
        try {
            body = json.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise envelope", e);
        }

        kafka.send(Topics.MATCHING_RIDE_ASSIGNED, rideId.toString(), body);
        log.info("Demo (kafka) eventId={} topic={} userId={}",
                eventId, Topics.MATCHING_RIDE_ASSIGNED, req.userId());
    }

    // ----- DTOs -----

    public enum Trigger { DIRECT, KAFKA }

    public record NotifyRequest(
            @NotNull UUID             userId,
            @NotNull Role             role,
            @NotNull NotificationType type,
            @NotNull Trigger          trigger
    ) {}
}
