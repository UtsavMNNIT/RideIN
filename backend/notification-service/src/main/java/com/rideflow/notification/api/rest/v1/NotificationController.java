package com.rideflow.notification.api.rest.v1;

import com.rideflow.notification.api.dto.response.NotificationResponse;
import com.rideflow.notification.application.usecase.FetchRecentNotificationsUseCase;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/**
 * Backfill endpoint used when a client reconnects after missing real-time
 * pushes. The WebSocket is at-most-once; combined with this endpoint the
 * effective guarantee is at-least-once delivery (clients may see a
 * notification both pushed and backfilled — UI dedupes by id).
 *
 * <p>{@code X-User-Id} is the authenticated rider/driver, injected by the API
 * gateway in production; for local development the client provides it.
 * Production wiring will replace this with claims from the JWT.
 */
@RestController
@RequestMapping("/v1/notifications")
public class NotificationController {

    private final FetchRecentNotificationsUseCase fetch;

    public NotificationController(FetchRecentNotificationsUseCase fetch) {
        this.fetch = fetch;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> recent(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(value = "since",  required = false) String sinceIso,
            @RequestParam(value = "limit",  required = false, defaultValue = "50") int limit) {

        Instant since = null;
        if (sinceIso != null && !sinceIso.isBlank()) {
            try {
                since = Instant.parse(sinceIso);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        List<NotificationResponse> body = fetch.fetchSince(userId, since, limit)
                .stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }
}
