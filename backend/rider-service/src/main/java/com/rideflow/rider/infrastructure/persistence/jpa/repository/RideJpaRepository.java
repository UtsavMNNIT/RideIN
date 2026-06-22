package com.rideflow.rider.infrastructure.persistence.jpa.repository;

import com.rideflow.rider.domain.model.RideStatus;
import com.rideflow.rider.infrastructure.persistence.jpa.entity.RideEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface RideJpaRepository extends JpaRepository<RideEntity, UUID> {

    // ---- operator metrics (admin dashboard) ----

    long countByStatusIn(Collection<RideStatus> statuses);

    long countByStatus(RideStatus status);

    @Query("""
           select count(distinct r.assignedDriverId) from RideEntity r
            where r.assignedDriverId is not null
              and r.status in :statuses
           """)
    long countDistinctActiveDrivers(@Param("statuses") Collection<RideStatus> statuses);

    /**
     * Rough avg seconds from request to the last projection update for rides that
     * advanced past REQUESTED — a cheap proxy for dispatch latency (rider-service
     * has no explicit assigned-at timestamp). Postgres-only (native).
     */
    @Query(value = """
           select coalesce(avg(extract(epoch from (updated_at - requested_at))), 0)
             from rider.rides
            where status in ('ASSIGNED', 'STARTED', 'COMPLETED')
           """, nativeQuery = true)
    double avgDispatchSeconds();

    @Query("""
           select r from RideEntity r
            where r.riderId = :riderId
            order by r.requestedAt desc
           """)
    List<RideEntity> findByRider(@Param("riderId") UUID riderId, Pageable page);

    @Query("""
           select r from RideEntity r
            where r.assignedDriverId = :driverId
              and r.status = :status
              and (:from is null or r.requestedAt >= :from)
              and (:to   is null or r.requestedAt <  :to)
            order by r.requestedAt desc
           """)
    List<RideEntity> findCompletedForDriver(@Param("driverId") UUID driverId,
                                            @Param("status") RideStatus status,
                                            @Param("from") Instant from,
                                            @Param("to") Instant to);
}
