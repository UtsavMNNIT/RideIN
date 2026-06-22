package com.rideflow.rider.infrastructure.persistence.jpa;

import com.rideflow.rider.application.port.out.OperatorMetricsRepository;
import com.rideflow.rider.domain.model.RideStatus;
import com.rideflow.rider.infrastructure.persistence.jpa.repository.RideJpaRepository;
import com.rideflow.rider.infrastructure.persistence.jpa.repository.RiderJpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA adapter for {@link OperatorMetricsRepository}. All counts come from the
 * rider-owned read-model (riders + the ride projection); "online drivers" is a
 * proxy (distinct drivers on an active ride) since rider-service does not track
 * driver availability.
 */
@Repository
public class JpaOperatorMetricsRepository implements OperatorMetricsRepository {

    private static final List<RideStatus> ACTIVE =
            List.of(RideStatus.REQUESTED, RideStatus.ASSIGNED, RideStatus.STARTED);
    private static final List<RideStatus> ACTIVE_WITH_DRIVER =
            List.of(RideStatus.ASSIGNED, RideStatus.STARTED);
    private static final List<RideStatus> UNSUCCESSFUL =
            List.of(RideStatus.CANCELLED, RideStatus.NO_DRIVERS_FOUND);

    private final RideJpaRepository  rides;
    private final RiderJpaRepository riders;

    public JpaOperatorMetricsRepository(RideJpaRepository rides, RiderJpaRepository riders) {
        this.rides  = rides;
        this.riders = riders;
    }

    @Override public long activeRideCount()        { return rides.countByStatusIn(ACTIVE); }
    @Override public long onlineDriverCount()       { return rides.countDistinctActiveDrivers(ACTIVE_WITH_DRIVER); }
    @Override public long completedRideCount()      { return rides.countByStatus(RideStatus.COMPLETED); }
    @Override public long unsuccessfulRideCount()   { return rides.countByStatusIn(UNSUCCESSFUL); }
    @Override public long totalRiders()             { return riders.count(); }
    @Override public double avgDispatchSeconds()    { return rides.avgDispatchSeconds(); }
}
