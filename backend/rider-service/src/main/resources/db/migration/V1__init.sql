-- =============================================================================
-- rider-service — schema baseline
-- Owner role: rider_user (created by postgres init script).
-- =============================================================================

CREATE TYPE rider.vehicle_type AS ENUM ('STANDARD', 'XL', 'PREMIUM');

-- Full ride lifecycle. rider-service only ever writes REQUESTED (and reads the
-- rest); downstream states are projected from matching/ride events in a later
-- slice. Declared up-front so the enum never needs a breaking migration.
CREATE TYPE rider.ride_status AS ENUM (
    'REQUESTED',
    'ASSIGNED',
    'STARTED',
    'COMPLETED',
    'CANCELLED'
);

-- -----------------------------------------------------------------------------
-- riders — the rider aggregate root (identity + credentials).
-- -----------------------------------------------------------------------------
CREATE TABLE rider.riders (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    phone         VARCHAR(20)  NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_riders_email ON rider.riders (email);
CREATE UNIQUE INDEX uq_riders_phone ON rider.riders (phone);

-- -----------------------------------------------------------------------------
-- rides — a ride request and its lifecycle state.
-- -----------------------------------------------------------------------------
CREATE TABLE rider.rides (
    id            UUID         PRIMARY KEY,
    rider_id      UUID         NOT NULL REFERENCES rider.riders (id),
    pickup_lat    DOUBLE PRECISION NOT NULL,
    pickup_lng    DOUBLE PRECISION NOT NULL,
    dropoff_lat   DOUBLE PRECISION NOT NULL,
    dropoff_lng   DOUBLE PRECISION NOT NULL,
    vehicle_type  rider.vehicle_type NOT NULL,
    status        rider.ride_status  NOT NULL DEFAULT 'REQUESTED',
    requested_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Ride-history read path: "rides for rider X, newest first".
CREATE INDEX idx_rides_rider_recent
    ON rider.rides (rider_id, requested_at DESC);

-- -----------------------------------------------------------------------------
-- outbox — transactional outbox for reliable event publishing.
-- A ride and its event row commit in one transaction; the relay drains
-- unpublished rows to Kafka at-least-once (consumers dedupe by event id).
-- -----------------------------------------------------------------------------
CREATE TABLE rider.outbox (
    id             UUID         PRIMARY KEY,    -- == EventEnvelope.eventId (dedupe key)
    aggregate_type VARCHAR(40)  NOT NULL,
    aggregate_id   UUID         NOT NULL,
    event_type     VARCHAR(80)  NOT NULL,
    topic          VARCHAR(120) NOT NULL,
    partition_key  VARCHAR(120) NOT NULL,
    payload        JSONB        NOT NULL,       -- the fully serialized EventEnvelope
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

-- Relay drain query: unpublished rows, oldest first.
CREATE INDEX idx_outbox_unpublished
    ON rider.outbox (created_at)
    WHERE published_at IS NULL;
