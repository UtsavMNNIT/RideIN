package com.rideflow.driver.infrastructure.persistence.jpa.repository;

import com.rideflow.driver.infrastructure.persistence.jpa.entity.ProcessedEventEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventJpaRepository
        extends JpaRepository<ProcessedEventEntity, ProcessedEventEntity.Key> {
}
