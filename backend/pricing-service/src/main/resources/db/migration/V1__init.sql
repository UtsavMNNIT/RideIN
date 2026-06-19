-- =============================================================================
-- pricing-service — schema baseline
-- Owner role: pricing_user (created by postgres init script).
-- Search path is set in application.yml (currentSchema=pricing).
-- =============================================================================

CREATE TYPE pricing.vehicle_type AS ENUM (
    'STANDARD',
    'XL',
    'PREMIUM'
);

-- -----------------------------------------------------------------------------
-- rate_cards: per-vehicle-type pricing parameters.
-- Stored in the DB (not config) so operations can retune pricing without a
-- redeploy. Loaded + cached by RateCardProvider.
-- -----------------------------------------------------------------------------
CREATE TABLE pricing.rate_cards (
    vehicle_type   pricing.vehicle_type PRIMARY KEY,
    currency       VARCHAR(3)    NOT NULL,
    base_fare      NUMERIC(10,2) NOT NULL,
    per_km         NUMERIC(10,2) NOT NULL,
    per_minute     NUMERIC(10,2) NOT NULL,
    minimum_fare   NUMERIC(10,2) NOT NULL,
    booking_fee    NUMERIC(10,2) NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT rate_cards_non_negative CHECK (
        base_fare >= 0 AND per_km >= 0 AND per_minute >= 0
        AND minimum_fare >= 0 AND booking_fee >= 0
    )
);

-- Seed rate cards (INR). Tune in production via an ops migration / admin path.
INSERT INTO pricing.rate_cards
    (vehicle_type, currency, base_fare, per_km, per_minute, minimum_fare, booking_fee)
VALUES
    ('STANDARD', 'INR', 30.00, 12.00, 1.50,  60.00, 10.00),
    ('XL',       'INR', 50.00, 18.00, 2.00,  90.00, 15.00),
    ('PREMIUM',  'INR', 80.00, 25.00, 3.00, 150.00, 20.00);

-- -----------------------------------------------------------------------------
-- quotes: every fare quote we compute, whether up-front (REST) or for a ride.
-- The full breakdown is stored for transparency, audit, and dispute handling.
-- -----------------------------------------------------------------------------
CREATE TABLE pricing.quotes (
    id                 UUID          PRIMARY KEY,
    ride_id            UUID,                       -- null for up-front estimates
    rider_id           UUID,
    pickup_lat         DOUBLE PRECISION NOT NULL,
    pickup_lng         DOUBLE PRECISION NOT NULL,
    dropoff_lat        DOUBLE PRECISION NOT NULL,
    dropoff_lng        DOUBLE PRECISION NOT NULL,
    vehicle_type       pricing.vehicle_type NOT NULL,
    currency           VARCHAR(3)    NOT NULL,
    base_fare          NUMERIC(10,2) NOT NULL,
    distance_fare      NUMERIC(10,2) NOT NULL,
    time_fare          NUMERIC(10,2) NOT NULL,
    subtotal           NUMERIC(10,2) NOT NULL,
    surge_multiplier   NUMERIC(4,2)  NOT NULL,
    surged_subtotal    NUMERIC(10,2) NOT NULL,
    booking_fee        NUMERIC(10,2) NOT NULL,
    total              NUMERIC(10,2) NOT NULL,
    est_distance_km    DOUBLE PRECISION NOT NULL,
    est_duration_min   DOUBLE PRECISION NOT NULL,
    valid_until        TIMESTAMPTZ   NOT NULL,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- One quote per ride: a redelivered RideRequested cannot create a second quote.
CREATE UNIQUE INDEX idx_quotes_ride ON pricing.quotes (ride_id) WHERE ride_id IS NOT NULL;
CREATE INDEX idx_quotes_rider       ON pricing.quotes (rider_id, created_at DESC);

-- -----------------------------------------------------------------------------
-- outbox: transactional outbox for cross-service event publication
-- -----------------------------------------------------------------------------
CREATE TABLE pricing.outbox (
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

CREATE INDEX idx_outbox_pending   ON pricing.outbox (occurred_at) WHERE published = FALSE;
CREATE INDEX idx_outbox_aggregate ON pricing.outbox (aggregate_id);

-- -----------------------------------------------------------------------------
-- processed_events: consumer-side idempotency table
-- -----------------------------------------------------------------------------
CREATE TABLE pricing.processed_events (
    event_id        UUID         NOT NULL,
    consumer_group  VARCHAR(64)  NOT NULL,
    processed_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer_group)
);
