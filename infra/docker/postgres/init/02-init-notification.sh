#!/usr/bin/env bash
# =============================================================================
# notification-service — schema + role.
# Runs after 01-init.sh on first container start. Idempotent.
# =============================================================================
set -euo pipefail

: "${POSTGRES_USER:?must be set by container env}"
: "${POSTGRES_DB:?must be set by container env}"
: "${NOTIFICATION_DB_PASSWORD:?must be set in compose env}"

echo "[rideflow-init] adding 'notification' schema + role..."

psql -v ON_ERROR_STOP=1 \
     --username "${POSTGRES_USER}" \
     --dbname   "${POSTGRES_DB}" <<-SQL
    CREATE SCHEMA IF NOT EXISTS notification;

    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'notification_user') THEN
            CREATE ROLE notification_user LOGIN PASSWORD '${NOTIFICATION_DB_PASSWORD}';
        ELSE
            ALTER  ROLE notification_user WITH LOGIN PASSWORD '${NOTIFICATION_DB_PASSWORD}';
        END IF;
    END
    \$\$;

    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO notification_user;
    GRANT USAGE   ON SCHEMA   notification   TO notification_user;
    GRANT CREATE  ON SCHEMA   notification   TO notification_user;

    ALTER DEFAULT PRIVILEGES IN SCHEMA notification
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO notification_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA notification
        GRANT USAGE, SELECT ON SEQUENCES TO notification_user;

    ALTER ROLE notification_user SET search_path = notification, public;
SQL

echo "[rideflow-init] notification schema ready."
