-- =============================================================================
-- matching-service — re-dispatch support
-- Adds a counter of how many times a ride has been re-dispatched after a
-- rejected/expired offer (consumed from ride.rejected). Bounded by
-- rideflow.dispatch.max-redispatches in application.yml.
-- =============================================================================

ALTER TABLE matching.rides
    ADD COLUMN redispatch_count INT NOT NULL DEFAULT 0;
