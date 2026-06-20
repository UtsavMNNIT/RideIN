#!/usr/bin/env bash
# =============================================================================
# RideFlow — Kafka topic bootstrap (one-shot, idempotent)
# Creates all Phase 0 topics with correct partitions, retention, cleanup policy.
# Re-runs are safe (--if-not-exists).
# =============================================================================
set -euo pipefail

BOOTSTRAP="${KAFKA_BOOTSTRAP:-kafka:9092}"
RF="${REPLICATION_FACTOR:-1}"

# Defensive wait — depends_on:service_healthy already gates us, but
# kafka-broker-api-versions is the canonical "broker actually ready" probe.
echo "[topics-init] waiting for kafka at ${BOOTSTRAP}..."
for i in $(seq 1 60); do
    if kafka-broker-api-versions --bootstrap-server "${BOOTSTRAP}" >/dev/null 2>&1; then
        echo "[topics-init] kafka is ready."
        break
    fi
    sleep 2
done

# create <name> <partitions> <retention_ms> [extra-configs]
create_topic() {
    local name="$1" parts="$2" retention="$3" extras="${4:-}"

    echo "[topics-init] ► ${name}  partitions=${parts}  retention_ms=${retention}  ${extras}"

    # shellcheck disable=SC2086
    kafka-topics --bootstrap-server "${BOOTSTRAP}" \
        --create --if-not-exists \
        --topic "${name}" \
        --partitions "${parts}" \
        --replication-factor "${RF}" \
        --config retention.ms="${retention}" \
        ${extras}
}

# ---- Time constants (ms) ----
H1=$((1 * 3600 * 1000))
D7=$((7  * 24 * 3600 * 1000))
D14=$((14 * 24 * 3600 * 1000))
D30=$((30 * 24 * 3600 * 1000))

# =============================================================================
# Phase 0 topic catalog
# Format:                       name                              parts  retention  extras
# =============================================================================
create_topic "rider.ride-requested"            12  "${D7}"
create_topic "matching.ride-assigned"          12  "${D7}"
create_topic "matching.ride-dispatch-failed"    6  "${D7}"
create_topic "driver.location-updated"         24  "${H1}"
create_topic "driver.availability-changed"      6  "${D30}"  "--config cleanup.policy=compact --config min.cleanable.dirty.ratio=0.1"
create_topic "ride.accepted"                   12  "${D7}"
create_topic "ride.rejected"                   12  "${D7}"
create_topic "ride.started"                    12  "${D30}"
create_topic "ride.completed"                  12  "${D30}"
create_topic "ride.cancelled"                  12  "${D30}"
create_topic "pricing.fare-quoted"             12  "${D7}"

# ---- Dead-letter queues ----
# One per topic that has a consumer (a poison message is routed to <topic>.DLQ;
# auto-create is disabled, so every consumed topic needs its DLQ provisioned).
for t in \
    rider.ride-requested \
    matching.ride-assigned \
    matching.ride-dispatch-failed \
    driver.location-updated \
    ride.accepted \
    ride.rejected \
    ride.started \
    ride.completed \
    ride.cancelled \
    pricing.fare-quoted
do
    create_topic "${t}.DLQ" 3 "${D14}"
done

echo "[topics-init] ----- final topic list -----"
kafka-topics --bootstrap-server "${BOOTSTRAP}" --list | sort
echo "[topics-init] done."
