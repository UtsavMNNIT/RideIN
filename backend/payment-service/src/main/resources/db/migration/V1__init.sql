-- =============================================================================
-- payment-service — schema baseline
-- Owner role: payment_user (created by postgres init script).
-- Search path is set in application.yml (currentSchema=payment).
--
-- payment-service simulates fare settlement. A payment is opened (PENDING) when
-- the fare is quoted, then settled when the ride completes. One payment per
-- ride: the unique ride_id makes a redelivered pricing.fare-quoted a no-op.
-- =============================================================================

CREATE TYPE payment.payment_status AS ENUM (
    'PENDING',
    'AUTHORIZED',
    'CAPTURED',
    'SETTLED',
    'FAILED',
    'CANCELLED'
);

-- -----------------------------------------------------------------------------
-- payment_methods: mock saved cards (no real PSP token — demo only).
-- -----------------------------------------------------------------------------
CREATE TABLE payment.payment_methods (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL,
    brand       VARCHAR(32)  NOT NULL,        -- VISA | MASTERCARD | AMEX | ...
    last4       VARCHAR(4)   NOT NULL,
    token       VARCHAR(64)  NOT NULL,        -- opaque mock token
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_methods_user ON payment.payment_methods (user_id, created_at DESC);

-- -----------------------------------------------------------------------------
-- payments: the settlement aggregate. One row per ride.
-- -----------------------------------------------------------------------------
CREATE TABLE payment.payments (
    id                 UUID         PRIMARY KEY,
    ride_id            UUID         NOT NULL UNIQUE,
    rider_id           UUID         NOT NULL,
    driver_id          UUID,                          -- known at settlement time
    amount             NUMERIC(12,2) NOT NULL,
    currency           VARCHAR(3)   NOT NULL,
    status             payment.payment_status NOT NULL,
    payment_method_id  UUID,
    failure_reason     VARCHAR(128),
    settled_at         TIMESTAMPTZ,
    version            INT          NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_rider ON payment.payments (rider_id, created_at DESC);

-- -----------------------------------------------------------------------------
-- outbox: transactional outbox for cross-service event publication
-- (copied verbatim from trip-service — published/attempt_count variant)
-- -----------------------------------------------------------------------------
CREATE TABLE payment.outbox (
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

CREATE INDEX idx_outbox_pending   ON payment.outbox (occurred_at) WHERE published = FALSE;
CREATE INDEX idx_outbox_aggregate ON payment.outbox (aggregate_id);

-- -----------------------------------------------------------------------------
-- processed_events: consumer-side idempotency table
-- -----------------------------------------------------------------------------
CREATE TABLE payment.processed_events (
    event_id        UUID         NOT NULL,
    consumer_group  VARCHAR(64)  NOT NULL,
    processed_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer_group)
);

-- -----------------------------------------------------------------------------
-- updated_at trigger
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION payment.tg_set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payment.payments
    FOR EACH ROW EXECUTE FUNCTION payment.tg_set_updated_at();
