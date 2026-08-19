package com.aedmp.platform.dataset;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "dataset_columns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_version_id", nullable = false)
    private DatasetVersion datasetVersion;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "expected_type")
    private String expectedType;

    @Column(name = "observed_type", nullable = false)
    private String observedType;

    @Column(name = "ordinal_position", nullable = false)
    private Integer ordinalPosition;

    @Column(name = "nullable_observed", nullable = false)
    @Builder.Default
    private Boolean nullableObserved = true;

    @Column(name = "distinct_count")
    private Long distinctCount;

    @Column(name = "null_count")
    private Long nullCount;

    @Column(name = "sample_values")
    private String sampleValues;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
