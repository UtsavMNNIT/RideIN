package com.rideflow.matching.infrastructure.persistence.jpa;

import com.rideflow.matching.application.port.out.ProcessedEventStore;
import com.rideflow.matching.infrastructure.persistence.jpa.entity.ProcessedEventEntity;
import com.rideflow.matching.infrastructure.persistence.jpa.repository.ProcessedEventJpaRepository;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/** JPA adapter for {@link ProcessedEventStore} over {@code matching.processed_events}. */
@Repository
public class JpaProcessedEventStore implements ProcessedEventStore {

    private final ProcessedEventJpaRepository repo;

    public JpaProcessedEventStore(ProcessedEventJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public boolean isProcessed(UUID eventId, String consumerGroup) {
        return repo.existsById(new ProcessedEventEntity.Key(eventId, consumerGroup));
    }

    @Override
    public void markProcessed(UUID eventId, String consumerGroup) {
        repo.save(new ProcessedEventEntity(eventId, consumerGroup, Instant.now()));
    }
}
