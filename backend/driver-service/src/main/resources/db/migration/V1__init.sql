-- =============================================================================
-- driver-service — schema baseline
-- Owner role: driver_user (created by postgres init script).
-- =============================================================================

-- Vehicle class offered by the driver. Kept in lock-step with the
-- com.rideflow.driver.domain.model.VehicleType enum.
CREATE TYPE driver.vehicle_type AS ENUM ('STANDARD', 'XL', 'PREMIUM');

-- Dispatch-facing presence state.
--   OFFLINE  — not accepting work; location pings rejected
--   ONLINE   — available for dispatch
--   ON_TRIP  — currently serving a ride (set by matching-service, reserved here)
CREATE TYPE driver.availability AS ENUM ('OFFLINE', 'ONLINE', 'ON_TRIP');

-- -----------------------------------------------------------------------------
-- drivers — the driver aggregate root.
-- Owns identity, credentials, vehicle profile, presence and last-known location.
-- -----------------------------------------------------------------------------
CREATE TABLE driver.drivers (
    id               UUID         PRIMARY KEY,
    email            VARCHAR(254) NOT NULL,
    phone            VARCHAR(20)  NOT NULL,
    full_name        VARCHAR(120) NOT NULL,
    password_hash    VARCHAR(100) NOT NULL,           -- BCrypt hash (60 chars), headroom kept
    vehicle_type     driver.vehicle_type  NOT NULL,
    vehicle_plate    VARCHAR(16)  NOT NULL,
    availability     driver.availability  NOT NULL DEFAULT 'OFFLINE',
    last_lat         DOUBLE PRECISION,                -- nullable until first location ping
    last_lng         DOUBLE PRECISION,
    last_location_at TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Login + registration uniqueness. Email/phone are stored normalized
-- (lower-cased / trimmed) by the application before persistence.
CREATE UNIQUE INDEX uq_drivers_email ON driver.drivers (email);
CREATE UNIQUE INDEX uq_drivers_phone ON driver.drivers (phone);

-- Dispatch lookup: "give me ONLINE drivers" (matching-service read path).
CREATE INDEX idx_drivers_availability
    ON driver.drivers (availability)
    WHERE availability = 'ONLINE';
