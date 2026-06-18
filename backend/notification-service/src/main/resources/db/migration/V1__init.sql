-- =============================================================================
-- notification-service — schema baseline
-- Owner role: notification_user (created by postgres init script).
-- =============================================================================

CREATE TYPE notification.recipient_role AS ENUM ('RIDER', 'DRIVER');

CREATE TYPE notification.notification_type AS ENUM (
    'RIDE_MATCHED',
    'RIDE_STARTED',
    'RIDE_COMPLETED'
);

-- -----------------------------------------------------------------------------
-- notifications — durable record of every notification we ever produced.
-- Used both as audit trail and as the offline-replay source for clients that
-- reconnect after missing a real-time push.
-- -----------------------------------------------------------------------------
CREATE TABLE notification.notifications (
    id              UUID         PRIMARY KEY,
    user_id         UUID         NOT NULL,
    role            notification.recipient_role NOT NULL,
    type            notification.notification_type NOT NULL,
    ride_id         UUID,
    payload         JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    delivered_at    TIMESTAMPTZ,                  -- set when a WS push observed the row
    read_at         TIMESTAMPTZ                   -- set when client acks via REST
);

-- Backfill path: "give me everything for user X since timestamp T".
CREATE INDEX idx_notifications_user_recent
    ON notification.notifications (user_id, created_at DESC);

-- Unread badge / inbox view.
CREATE INDEX idx_notifications_user_unread
    ON notification.notifications (user_id, created_at DESC)
    WHERE read_at IS NULL;

-- Inverse-lookup by ride (e.g. "show me all notifications for this ride").
CREATE INDEX idx_notifications_ride
    ON notification.notifications (ride_id)
    WHERE ride_id IS NOT NULL;

-- -----------------------------------------------------------------------------
-- processed_events — consumer-side idempotency.
-- A redelivered Kafka message with the same event_id is silently swallowed.
-- -----------------------------------------------------------------------------
CREATE TABLE notification.processed_events (
    event_id        UUID         NOT NULL,
    consumer_group  VARCHAR(64)  NOT NULL,
    processed_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer_group)
);
