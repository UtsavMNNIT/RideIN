-- =============================================================================
-- remove-driver.lua
-- Atomic, unconditional removal of a single driver from the geo index.
--
-- Used when a driver goes OFFLINE: the driver must vanish from every shard so
-- matching never offers it a ride. Unlike evict-stale-driver.lua there is NO
-- heartbeat-score guard — an explicit OFFLINE event is authoritative.
--
-- KEYS:
--   KEYS[1 .. N-2] = every geo shard key
--                    (geo:drivers:available:<VT> and geo:drivers:busy:<VT>
--                     for each vehicle type)
--   KEYS[N-1]      = heartbeat-zset-key  e.g. driver:heartbeat
--   KEYS[N]        = meta-hash-key        e.g. driver:meta:<driverId>
--
-- ARGV:
--   ARGV[1] = driverId   (string)
--
-- Returns:
--   1 always (ZREM/DEL are no-ops if the member is already absent — idempotent).
--
-- Keys are constructed by the caller, not inside the script, to stay
-- Redis-Cluster compatible (single-node today).
-- =============================================================================

local driverId     = ARGV[1]
local heartbeatKey = KEYS[#KEYS - 1]
local metaKey      = KEYS[#KEYS]

-- ---- Remove from every geo shard (ZREM is a no-op if absent) -------------
for i = 1, #KEYS - 2 do
    redis.call('ZREM', KEYS[i], driverId)
end

-- ---- Drop metadata + heartbeat ------------------------------------------
redis.call('DEL',  metaKey)
redis.call('ZREM', heartbeatKey, driverId)

return 1
