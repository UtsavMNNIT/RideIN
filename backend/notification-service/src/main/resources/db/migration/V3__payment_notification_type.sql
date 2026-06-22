-- =============================================================================
-- notification-service — add PAYMENT_SETTLED notification type
-- The `type` column is a Postgres enum (notification.notification_type); a new
-- NotificationType value must exist in the enum before any row can use it.
-- ADD VALUE is additive + idempotent (IF NOT EXISTS).
-- =============================================================================

ALTER TYPE notification.notification_type ADD VALUE IF NOT EXISTS 'PAYMENT_SETTLED';
