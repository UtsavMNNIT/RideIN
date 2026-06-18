#!/usr/bin/env bash
# =============================================================================
# RideFlow — Postgres bootstrap
# Runs on first container start only (when PGDATA is empty).
# Creates extensions, per-service schemas, and least-privilege roles.
# =============================================================================
set -euo pipefail

: "${POSTGRES_USER:?must be set by container env}"
: "${POSTGRES_DB:?must be set by container env}"
: "${RIDER_DB_PASSWORD:?must be set in compose env}"
: "${DRIVER_DB_PASSWORD:?must be set in compose env}"
: "${MATCHING_DB_PASSWORD:?must be set in compose env}"
: "${PRICING_DB_PASSWORD:?must be set in compose env}"

echo "[rideflow-init] bootstrapping database '${POSTGRES_DB}'..."

psql -v ON_ERROR_STOP=1 \
     --username "${POSTGRES_USER}" \
     --dbname   "${POSTGRES_DB}" <<-SQL
    -- -------------------------------------------------------------------
    -- Extensions
    -- -------------------------------------------------------------------
    CREATE EXTENSION IF NOT EXISTS citext;
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS pgcrypto;
    CREATE EXTENSION IF NOT EXISTS btree_gist;

    -- -------------------------------------------------------------------
    -- Schemas (one per service owning persistent state)
    -- -------------------------------------------------------------------
    CREATE SCHEMA IF NOT EXISTS rider;
    CREATE SCHEMA IF NOT EXISTS driver;
    CREATE SCHEMA IF NOT EXISTS matching;
    CREATE SCHEMA IF NOT EXISTS pricing;
SQL

# -------------------------------------------------------------------
# Per-service roles + grants (idempotent via DO blocks)
# -------------------------------------------------------------------
create_role() {
    local role="$1" password="$2" schema="$3"

    psql -v ON_ERROR_STOP=1 \
         --username "${POSTGRES_USER}" \
         --dbname   "${POSTGRES_DB}" <<-SQL
        DO \$\$
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${role}') THEN
                CREATE ROLE ${role} LOGIN PASSWORD '${password}';
            ELSE
                ALTER  ROLE ${role} WITH LOGIN PASSWORD '${password}';
            END IF;
        END
        \$\$;

        GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${role};
        GRANT USAGE   ON SCHEMA   ${schema}     TO ${role};
        GRANT CREATE  ON SCHEMA   ${schema}     TO ${role};

        -- Future tables/sequences created by this role auto-belong to it;
        -- also grant on any tables a migration tool creates as superuser:
        ALTER DEFAULT PRIVILEGES IN SCHEMA ${schema}
            GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${role};
        ALTER DEFAULT PRIVILEGES IN SCHEMA ${schema}
            GRANT USAGE, SELECT ON SEQUENCES TO ${role};

        ALTER ROLE ${role} SET search_path = ${schema}, public;
SQL
}

create_role rider_user    "${RIDER_DB_PASSWORD}"    rider
create_role driver_user   "${DRIVER_DB_PASSWORD}"   driver
create_role matching_user "${MATCHING_DB_PASSWORD}" matching
create_role pricing_user  "${PRICING_DB_PASSWORD}"  pricing

echo "[rideflow-init] done. schemas: rider, driver, matching, pricing"
