package com.rideflow.notification.infrastructure.persistence.jpa.repository;

import com.rideflow.notification.infrastructure.persistence.jpa.entity.NotificationEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

    @Query("""
           select n from NotificationEntity n
            where n.userId = :userId
              and n.readAt is null
            order by n.createdAt desc
           """)
    List<NotificationEntity> findUnreadForUser(@Param("userId") UUID userId, Pageable page);

    @Query("""
           select n from NotificationEntity n
            where n.userId = :userId
              and n.createdAt > :since
            order by n.createdAt desc
           """)
    List<NotificationEntity> findSince(@Param("userId") UUID userId,
                                       @Param("since")  Instant since,
                                       Pageable page);
}
