-- =============================================================================
-- mark-driver-busy.lua
-- Atomically move a driver from the AVAILABLE shard to the BUSY shard, in place
-- (re-using the coordinates already stored in the geo index), without needing a
-- fresh location reading.
--
-- Used when a driver goes ON_TRIP but the availability-changed event carried no
-- location (e.g. the driver had not pinged yet, or we choose not to trust a
-- possibly-stale coordinate from the event). The driver simply changes shard so
-- matching stops offering it rides; the next location ping re-confirms the spot.
--
-- KEYS:
--   KEYS[1] = available-geo-key  e.g. geo:drivers:available:STANDARD
--   KEYS[2] = busy-geo-key       e.g. geo:drivers:busy:STANDARD
--   KEYS[3] = meta-hash-key      e.g. driver:meta:<driverId>
--
-- ARGV:
--   ARGV[1] = driverId   (string)
--
-- Returns:
--   1 if the driver was moved (was present in the available shard);
--   0 if the driver was not in the available shard (nothing to move).
--
-- Keys are constructed by the caller, not inside the script, to stay
-- Redis-Cluster compatible (single-node today).
-- =============================================================================

local driverId = ARGV[1]

-- ---- Read current position from the AVAILABLE shard ----------------------
local pos = redis.call('GEOPOS', KEYS[1], driverId)
if not pos or not pos[1] then
    -- Not in the available shard: either already busy, or we have never indexed
    -- this driver. Nothing to do here; the next location ping will place it.
    return 0
end

local lng = pos[1][1]
local lat = pos[1][2]

-- ---- Move to BUSY shard --------------------------------------------------
redis.call('GEOADD', KEYS[2], lng, lat, driverId)
redis.call('ZREM',   KEYS[1], driverId)

-- ---- Reflect new availability in metadata (best-effort) ------------------
if redis.call('EXISTS', KEYS[3]) == 1 then
    redis.call('HSET', KEYS[3], 'availability', 'ON_TRIP')
end

return 1
