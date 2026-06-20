-- =============================================================================
-- driver-service — consumer-side idempotency ledger.
-- Added when driver-service began consuming ride-lifecycle events to sync
-- availability (ON_TRIP on accept; back to ONLINE on complete/cancel).
-- =============================================================================
CREATE TABLE driver.processed_events (
    event_id        UUID         NOT NULL,
    consumer_group  VARCHAR(64)  NOT NULL,
    processed_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer_group)
);
