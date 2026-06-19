-- =============================================================================
-- trip-service — schema baseline
-- Owner role: trip_user (created by postgres init script).
-- Search path is set in application.yml (currentSchema=trip).
--
-- trip-service owns the post-assignment lifecycle. A trip's id IS the upstream
-- rideId, so a redelivered matching.ride-assigned collides on the PK and is a
-- no-op (belt to the processed_events braces).
-- =============================================================================

CREATE TYPE trip.trip_status AS ENUM (
    'OFFERED',
    'ACCEPTED',
    'ARRIVED',
    'STARTED',
    'COMPLETED',
    'REJECTED',
    'EXPIRED',
    'CANCELLED'
);

CREATE TYPE trip.cancelled_by AS ENUM (
    'RIDER',
    'DRIVER',
    'SYSTEM'
);

-- -----------------------------------------------------------------------------
-- trips: the lifecycle aggregate root (single source of truth for ride state
-- after assignment).
-- -----------------------------------------------------------------------------
CREATE TABLE trip.trips (
    id                     UUID         PRIMARY KEY,   -- = rideId minted upstream
    rider_id               UUID         NOT NULL,
    driver_id              UUID         NOT NULL,       -- known at OFFER time
    pickup_lat             DOUBLE PRECISION NOT NULL,
    pickup_lng             DOUBLE PRECISION NOT NULL,
    dropoff_lat            DOUBLE PRECISION NOT NULL,
    dropoff_lng            DOUBLE PRECISION NOT NULL,
    vehicle_type           VARCHAR(16)  NOT NULL,
    status                 trip.trip_status NOT NULL,
    match_score            DOUBLE PRECISION,
    reject_reason          VARCHAR(32),                 -- DRIVER_DECLINED | EXPIRED
    cancelled_by           trip.cancelled_by,
    cancel_reason          VARCHAR(128),
    final_distance_meters  INT,
    final_duration_seconds INT,
    offered_at             TIMESTAMPTZ  NOT NULL,
    offer_expires_at       TIMESTAMPTZ  NOT NULL,
    accepted_at            TIMESTAMPTZ,
    arrived_at             TIMESTAMPTZ,
    started_at             TIMESTAMPTZ,
    completed_at           TIMESTAMPTZ,
    rejected_at            TIMESTAMPTZ,                 -- set for REJECTED and EXPIRED
    cancelled_at           TIMESTAMPTZ,
    version                INT          NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- One live trip per driver. Mirrors matching's partial-unique guard: even if a
-- bug tried to offer a second concurrent trip to a busy driver, the DB refuses.
CREATE UNIQUE INDEX uq_trips_active_driver ON trip.trips (driver_id)
    WHERE status IN ('OFFERED', 'ACCEPTED', 'ARRIVED', 'STARTED');

CREATE INDEX idx_trips_rider        ON trip.trips (rider_id, offered_at DESC);
-- Backs the offer-expiry sweeper's "find timed-out offers" scan.
CREATE INDEX idx_trips_offer_expiry ON trip.trips (offer_expires_at)
    WHERE status = 'OFFERED';

-- -----------------------------------------------------------------------------
-- outbox: transactional outbox for cross-service event publication
-- (copied verbatim from matching-service — published/attempt_count variant)
-- -----------------------------------------------------------------------------
CREATE TABLE trip.outbox (
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

CREATE INDEX idx_outbox_pending   ON trip.outbox (occurred_at) WHERE published = FALSE;
CREATE INDEX idx_outbox_aggregate ON trip.outbox (aggregate_id);

-- -----------------------------------------------------------------------------
-- processed_events: consumer-side idempotency table
-- -----------------------------------------------------------------------------
CREATE TABLE trip.processed_events (
    event_id        UUID         NOT NULL,
    consumer_group  VARCHAR(64)  NOT NULL,
    processed_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer_group)
);

-- -----------------------------------------------------------------------------
-- updated_at trigger
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION trip.tg_set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_trips_updated_at
    BEFORE UPDATE ON trip.trips
    FOR EACH ROW EXECUTE FUNCTION trip.tg_set_updated_at();
