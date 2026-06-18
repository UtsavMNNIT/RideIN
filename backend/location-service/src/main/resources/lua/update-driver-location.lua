-- =============================================================================
-- update-driver-location.lua
-- Atomic per-driver upsert of geo index + metadata + heartbeat.
--
-- KEYS:
--   KEYS[1] = available-geo-key  e.g. geo:drivers:available:STANDARD
--   KEYS[2] = busy-geo-key       e.g. geo:drivers:busy:STANDARD
--   KEYS[3] = meta-hash-key      e.g. driver:meta:<driverId>
--   KEYS[4] = heartbeat-zset-key e.g. driver:heartbeat
--
-- ARGV:
--   ARGV[1] = driverId          (string)
--   ARGV[2] = lng               (number)
--   ARGV[3] = lat               (number)
--   ARGV[4] = headingDegrees    (number | "")
--   ARGV[5] = speedMps          (number | "")
--   ARGV[6] = availability      ("ONLINE" | "ON_TRIP")
--   ARGV[7] = vehicleType       (string)
--   ARGV[8] = capturedAtEpochMs (number, string)
--   ARGV[9] = metaTtlSeconds    (number, string)
--
-- Returns:
--   1 if applied; 0 if dropped as stale (older than stored capturedAt).
--
-- All keys are passed by the caller so this script remains Redis-Cluster
-- compatible if we ever shard later (single-node today).
-- =============================================================================

local driverId      = ARGV[1]
local lng           = tonumber(ARGV[2])
local lat           = tonumber(ARGV[3])
local heading       = ARGV[4]
local speed         = ARGV[5]
local availability  = ARGV[6]
local vehicleType   = ARGV[7]
local capturedAt    = tonumber(ARGV[8])
local metaTtl       = tonumber(ARGV[9])

-- ---- Out-of-order guard --------------------------------------------------
local prevCapturedAt = redis.call('HGET', KEYS[3], 'capturedAt')
if prevCapturedAt and tonumber(prevCapturedAt) >= capturedAt then
    return 0
end

-- ---- Choose target geo shard --------------------------------------------
local targetGeo, otherGeo
if availability == 'ON_TRIP' then
    targetGeo, otherGeo = KEYS[2], KEYS[1]
else
    targetGeo, otherGeo = KEYS[1], KEYS[2]
end

-- ---- Update geo index ----------------------------------------------------
redis.call('GEOADD', targetGeo, lng, lat, driverId)
redis.call('ZREM',   otherGeo,  driverId)

-- ---- Update metadata hash -----------------------------------------------
redis.call('HSET', KEYS[3],
    'lng',          ARGV[2],
    'lat',          ARGV[3],
    'heading',      heading,
    'speed',        speed,
    'availability', availability,
    'vehicleType',  vehicleType,
    'capturedAt',   ARGV[8])
redis.call('EXPIRE', KEYS[3], metaTtl)

-- ---- Update heartbeat zset (score = capturedAt) -------------------------
redis.call('ZADD', KEYS[4], capturedAt, driverId)

return 1
