package com.rideflow.rider.infrastructure.persistence.jpa.repository;

import com.rideflow.rider.domain.model.RideStatus;
import com.rideflow.rider.infrastructure.persistence.jpa.entity.RideEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface RideJpaRepository extends JpaRepository<RideEntity, UUID> {

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
