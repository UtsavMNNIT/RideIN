package com.rideflow.matching.infrastructure.persistence.jpa.entity;

import com.rideflow.matching.domain.model.DispatchOutcome;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA mapping for {@code matching.dispatch_attempts} — the per-ride audit trail
 * of each radius-expansion pass. {@code ride_id} is stored as a plain column
 * (no JPA association) to keep writes simple and lazy-load-free.
 */
@Entity
@Table(name = "dispatch_attempts", schema = "matching")
public class DispatchAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // BIGSERIAL
    private Long id;

    @Column(name = "ride_id", nullable = false)
    private UUID rideId;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "radius_meters", nullable = false)
    private int radiusMeters;

    @Column(name = "candidates_found", nullable = false)
    private int candidatesFound;

    @Column(name = "selected_driver_id")
    private UUID selectedDriverId;

    @Column(name = "selected_score")
    private Double selectedScore;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private DispatchOutcome outcome;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected DispatchAttemptEntity() {}

    public DispatchAttemptEntity(UUID rideId, int attemptNo, int radiusMeters,
                                 int candidatesFound, UUID selectedDriverId, Double selectedScore,
                                 DispatchOutcome outcome, Integer durationMs, Instant attemptedAt) {
        this.rideId           = rideId;
        this.attemptNo        = attemptNo;
        this.radiusMeters     = radiusMeters;
        this.candidatesFound  = candidatesFound;
        this.selectedDriverId = selectedDriverId;
        this.selectedScore    = selectedScore;
        this.outcome          = outcome;
        this.durationMs       = durationMs;
        this.attemptedAt      = attemptedAt;
    }

    public Long getId() { return id; }
}
