-- =============================================================================
-- notification-service — add lifecycle notification types
-- The `type` column is a Postgres enum (notification.notification_type) mapped
-- via @JdbcTypeCode(NAMED_ENUM), so new NotificationType values must be added to
-- the enum before any row can use them.
--
-- ADD VALUE is additive and safe; IF NOT EXISTS keeps the migration idempotent.
-- (PostgreSQL 12+ permits ADD VALUE inside a transaction as long as the value is
--  not *used* in the same transaction — these migrations only add it.)
-- =============================================================================

ALTER TYPE notification.notification_type ADD VALUE IF NOT EXISTS 'NO_DRIVERS_FOUND';
ALTER TYPE notification.notification_type ADD VALUE IF NOT EXISTS 'RIDE_CANCELLED';
