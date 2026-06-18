package com.rideflow.rider.infrastructure.persistence.jpa.repository;

import com.rideflow.rider.infrastructure.persistence.jpa.entity.OutboxEvent;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxJpaRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("""
           select o from OutboxEvent o
            where o.publishedAt is null
            order by o.createdAt asc
           """)
    List<OutboxEvent> findUnpublished(Pageable page);
}
