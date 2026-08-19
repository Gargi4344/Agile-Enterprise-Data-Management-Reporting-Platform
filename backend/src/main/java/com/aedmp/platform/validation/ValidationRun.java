package com.aedmp.platform.validation;

import com.aedmp.platform.dataset.DatasetVersion;
import com.aedmp.platform.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "validation_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_version_id", nullable = false)
    private DatasetVersion datasetVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ValidationRunStatus status = ValidationRunStatus.RUNNING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "triggered_by", nullable = false)
    private User triggeredBy;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "total_records", nullable = false)
    @Builder.Default
    private Long totalRecords = 0L;

    @Column(name = "accepted_records", nullable = false)
    @Builder.Default
    private Long acceptedRecords = 0L;

    @Column(name = "rejected_records", nullable = false)
    @Builder.Default
    private Long rejectedRecords = 0L;

    @Column(name = "quality_score", precision = 5, scale = 2)
    private BigDecimal qualityScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (startedAt == null) {
            startedAt = createdAt;
        }
    }
}
