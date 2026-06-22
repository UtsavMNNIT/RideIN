package com.rideflow.matching.domain.repository;

import com.rideflow.matching.domain.model.DispatchCandidate;
import com.rideflow.matching.domain.model.GeoPoint;
import com.rideflow.matching.domain.model.VehicleType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Outbound port: supply nearby, dispatchable driver candidates around a pickup.
 *
 * <p>Implemented against the same Redis-Geo index that location-service writes
 * to ({@code geo:drivers:available:<vehicleType>} + {@code driver:meta:<id>}).
 * matching-service is a pure <em>reader</em> of that index — it never writes it.
 *
 * <p><b>Contract.</b> The returned list is already filtered to drivers that are:
 * <ul>
 *   <li>of the requested {@link VehicleType} (correct geo shard),</li>
 *   <li>within {@code radiusMeters} of the pickup,</li>
 *   <li>still {@code ONLINE} per their metadata (re-checked, not assumed from
 *       the shard), and</li>
 *   <li>fresh — their last heartbeat is within the staleness window.</li>
 * </ul>
 * Results carry the geo distance and (when known) heading/speed, but are
 * <em>unscored</em> — ranking is the scorer's job. Order is unspecified.
 */
public interface DispatchCandidateProvider {

    /**
     * @param pickup             ride pickup location
     * @param vehicleType        requested vehicle class (selects the geo shard)
     * @param radiusMeters       search radius for this attempt
     * @param limit              max candidates to return (K-nearest)
     * @param excludedDriverIds  drivers to omit — e.g. one who just rejected/let
     *                           an offer expire on a re-dispatch; empty on the
     *                           initial dispatch
     * @return filtered, unscored candidates; empty if none qualify
     */
    List<DispatchCandidate> findCandidates(GeoPoint pickup,
                                           VehicleType vehicleType,
                                           int radiusMeters,
                                           int limit,
                                           Set<UUID> excludedDriverIds);
}
