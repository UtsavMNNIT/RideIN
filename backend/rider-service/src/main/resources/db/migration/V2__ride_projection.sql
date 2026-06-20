-- =============================================================================
-- rider-service V2 — Ride read-projection
-- rider-service now consumes downstream events to project ride state past
-- REQUESTED, attach the assigned driver and the quoted fare, and dedupe.
-- =============================================================================

-- New terminal state for "dispatch found no driver". On PG 12+ ADD VALUE is
-- allowed inside the migration transaction as long as it is not USED in the
-- same transaction (it isn't here).
ALTER TYPE rider.ride_status ADD VALUE IF NOT EXISTS 'NO_DRIVERS_FOUND';

ALTER TABLE rider.rides
    ADD COLUMN assigned_driver_id     UUID,
    ADD COLUMN match_score            DOUBLE PRECISION,
    ADD COLUMN fare_total             NUMERIC(12,2),
    ADD COLUMN currency               VARCHAR(3),
    ADD COLUMN final_distance_meters  INT,
    ADD COLUMN final_duration_seconds INT;

-- Consumer-side idempotency for the projection consumer.
CREATE TABLE rider.processed_events (
    event_id        UUID         NOT NULL,
    consumer_group  VARCHAR(64)  NOT NULL,
    processed_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer_group)
);
