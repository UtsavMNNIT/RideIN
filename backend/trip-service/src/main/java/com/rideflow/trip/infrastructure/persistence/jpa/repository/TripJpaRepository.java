package com.rideflow.trip.infrastructure.persistence.jpa.repository;

import com.rideflow.trip.infrastructure.persistence.jpa.entity.TripEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TripJpaRepository extends JpaRepository<TripEntity, UUID> {

    /** Offer ids whose TTL has lapsed and are still OFFERED (backs the sweeper). */
    @Query("""
           select t.id from TripEntity t
            where t.status = com.rideflow.trip.domain.model.TripStatus.OFFERED
              and t.offerExpiresAt <= :cutoff
            order by t.offerExpiresAt asc
           """)
    List<UUID> findExpiredOfferIds(Instant cutoff, Pageable page);
}
