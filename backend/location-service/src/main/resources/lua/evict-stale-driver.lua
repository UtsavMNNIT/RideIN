-- =============================================================================
-- evict-stale-driver.lua
-- Atomic compare-and-remove of a single stale driver across all geo shards.
--
-- A driver is evicted only if its heartbeat score is still <= threshold at the
-- moment of removal. This closes the race where a driver emits a fresh location
-- event between the sweeper's scan and this call — we must not evict a driver
-- who just came back.
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
--   ARGV[2] = threshold  (epoch ms; evict only if heartbeat score <= threshold)
--
-- Returns:
--   1 if the driver was evicted; 0 if it was kept (refreshed) or already gone.
--
-- Keys are constructed by the caller, not inside the script, to stay
-- Redis-Cluster compatible (single-node today).
-- =============================================================================

local driverId  = ARGV[1]
local threshold = tonumber(ARGV[2])

local heartbeatKey = KEYS[#KEYS - 1]
local metaKey      = KEYS[#KEYS]

-- ---- Compare: is the driver still stale? --------------------------------
local score = redis.call('ZSCORE', heartbeatKey, driverId)
if not score then
    return 0                      -- already swept by another pod
end
if tonumber(score) > threshold then
    return 0                      -- refreshed since the scan — keep it
end

-- ---- Remove from every geo shard (ZREM is a no-op if absent) -------------
for i = 1, #KEYS - 2 do
    redis.call('ZREM', KEYS[i], driverId)
end

-- ---- Drop metadata + heartbeat ------------------------------------------
redis.call('DEL',  metaKey)
redis.call('ZREM', heartbeatKey, driverId)

return 1
