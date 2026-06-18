package com.rideflow.notification.infrastructure.persistence.jpa;

import com.rideflow.notification.application.port.out.ProcessedEventRepository;
import com.rideflow.notification.infrastructure.persistence.jpa.entity.ProcessedEventEntity;
import com.rideflow.notification.infrastructure.persistence.jpa.repository.ProcessedEventJpaRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA adapter for {@link ProcessedEventRepository}. The PK on
 * {@code (event_id, consumer_group)} is the locking primitive; a concurrent
 * insert from a redelivered message lands as
 * {@link DataIntegrityViolationException}, which we translate to {@code false}
 * so the caller can short-circuit gracefully.
 */
@Repository
public class JpaProcessedEventRepository implements ProcessedEventRepository {

    private final ProcessedEventJpaRepository jpa;

    public JpaProcessedEventRepository(ProcessedEventJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean markProcessed(UUID eventId, String consumerGroup) {
        try {
            jpa.save(new ProcessedEventEntity(eventId, consumerGroup, Instant.now()));
            return true;
        } catch (DataIntegrityViolationException dup) {
            return false;
        }
    }
}
