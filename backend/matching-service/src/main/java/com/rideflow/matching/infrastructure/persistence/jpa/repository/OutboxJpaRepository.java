package com.rideflow.matching.infrastructure.persistence.jpa.repository;

import com.rideflow.matching.infrastructure.persistence.jpa.entity.OutboxEvent;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxJpaRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Unpublished rows that have not yet exhausted their retry budget, oldest
     * first (preserves per-key order). Backed by {@code idx_outbox_pending}.
     */
    @Query("""
           select o from OutboxEvent o
            where o.published = false
              and o.attemptCount < :maxAttempts
            order by o.occurredAt asc
           """)
    List<OutboxEvent> findPublishable(int maxAttempts, Pageable page);
}
