package com.aedmp.platform.report;

import com.aedmp.platform.dataset.Dataset;
import com.aedmp.platform.dataset.DatasetVersion;
import com.aedmp.platform.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_version_id")
    private DatasetVersion datasetVersion;

    @Column(name = "report_type", nullable = false, length = 30)
    @Builder.Default
    private String reportType = "DATASET_QUALITY";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.GENERATED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode parameters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary_metrics", nullable = false, columnDefinition = "jsonb")
    private JsonNode summaryMetrics;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (generatedAt == null) {
            generatedAt = createdAt;
        }
    }
}
