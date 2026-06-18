-- =============================================================================
-- matching-service — schema baseline
-- Owner role: matching_user (created by postgres init script).
-- Search path is set in application.yml (currentSchema=matching).
-- =============================================================================

CREATE TYPE matching.ride_status AS ENUM (
    'REQUESTED',
    'DISPATCHING',
    'ASSIGNED',
    'DISPATCH_FAILED',
    'CANCELLED'
);

CREATE TYPE matching.dispatch_outcome AS ENUM (
    'SUCCESS',
    'NO_CANDIDATES',
    'ERROR'
);

-- -----------------------------------------------------------------------------
-- rides: aggregate root, source of truth for dispatch state machine
-- -----------------------------------------------------------------------------
CREATE TABLE matching.rides (
    id                  UUID         PRIMARY KEY,
    rider_id            UUID         NOT NULL,
    pickup_lat          DOUBLE PRECISION NOT NULL,
    pickup_lng          DOUBLE PRECISION NOT NULL,
    dropoff_lat         DOUBLE PRECISION NOT NULL,
    dropoff_lng         DOUBLE PRECISION NOT NULL,
    vehicle_type        VARCHAR(16)  NOT NULL,
    status              matching.ride_status NOT NULL,
    assigned_driver_id  UUID,
    assignment_score    DOUBLE PRECISION,
    failure_reason      VARCHAR(64),
    requested_at        TIMESTAMPTZ  NOT NULL,
    assigned_at         TIMESTAMPTZ,
    failed_at           TIMESTAMPTZ,
    version             INT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_rides_rider          ON matching.rides (rider_id, requested_at DESC);
CREATE INDEX idx_rides_status_pending ON matching.rides (status, requested_at)
                                        WHERE status IN ('REQUESTED', 'DISPATCHING');
CREATE INDEX idx_rides_assigned       ON matching.rides (assigned_driver_id)
                                        WHERE status = 'ASSIGNED';

-- -----------------------------------------------------------------------------
-- dispatch_attempts: audit trail of every radius expansion + candidate count
-- -----------------------------------------------------------------------------
CREATE TABLE matching.dispatch_attempts (
    id                 BIGSERIAL    PRIMARY KEY,
    ride_id            UUID         NOT NULL REFERENCES matching.rides(id) ON DELETE CASCADE,
    attempt_no         INT          NOT NULL,
    radius_meters      INT          NOT NULL,
    candidates_found   INT          NOT NULL,
    selected_driver_id UUID,
    selected_score     DOUBLE PRECISION,
    outcome            matching.dispatch_outcome NOT NULL,
    duration_ms        INT,
    attempted_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_dispatch_attempts_ride ON matching.dispatch_attempts (ride_id, attempt_no);

-- -----------------------------------------------------------------------------
-- outbox: transactional outbox for cross-service event publication
-- -----------------------------------------------------------------------------
CREATE TABLE matching.outbox (
    id              BIGSERIAL    PRIMARY KEY,
    event_id        UUID         NOT NULL UNIQUE,
    aggregate_id    UUID         NOT NULL,
    event_type      VARCHAR(64)  NOT NULL,
    topic           VARCHAR(128) NOT NULL,
    partition_key   VARCHAR(64)  NOT NULL,
    payload         JSONB        NOT NULL,
    occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published       BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMPTZ,
    attempt_count   INT          NOT NULL DEFAULT 0,
    last_error      TEXT
);

-- Partial index makes "find unpublished rows" an index-only scan,
-- regardless of how many published rows accumulate.
CREATE INDEX idx_outbox_pending ON matching.outbox (occurred_at)
                                  WHERE published = FALSE;
CREATE INDEX idx_outbox_aggregate ON matching.outbox (aggregate_id);

-- -----------------------------------------------------------------------------
-- processed_events: consumer-side idempotency table
-- -----------------------------------------------------------------------------
CREATE TABLE matching.processed_events (
    event_id        UUID         NOT NULL,
    consumer_group  VARCHAR(64)  NOT NULL,
    processed_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer_group)
);

-- Optional TTL strategy: a cron / pg_partman job purges rows older than 30 days.
-- Documented in docs/runbooks/processed-events-cleanup.md (Phase 9).

-- -----------------------------------------------------------------------------
-- updated_at trigger
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION matching.tg_set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_rides_updated_at
    BEFORE UPDATE ON matching.rides
    FOR EACH ROW EXECUTE FUNCTION matching.tg_set_updated_at();
