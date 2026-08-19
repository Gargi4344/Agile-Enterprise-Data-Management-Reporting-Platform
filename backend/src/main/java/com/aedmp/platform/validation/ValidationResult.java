package com.aedmp.platform.validation;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "validation_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "validation_run_id", nullable = false)
    private ValidationRun validationRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private ValidationRule rule;

    @Column(name = "row_number")
    private Long rowNumber;

    @Column(name = "column_name")
    private String columnName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RuleSeverity severity;

    @Column(nullable = false)
    private Boolean passed;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "sample_value", length = 500)
    private String sampleValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
