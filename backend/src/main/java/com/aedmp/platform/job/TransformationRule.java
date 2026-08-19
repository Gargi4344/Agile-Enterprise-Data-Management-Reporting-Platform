package com.aedmp.platform.job;

import com.aedmp.platform.dataset.Dataset;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "transformation_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransformationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;

    @Column(name = "transformation_type", nullable = false, length = 30)
    private String transformationType;

    @Column(nullable = false, length = 2000)
    private String expression;

    @Column(name = "ordinal_position", nullable = false)
    @Builder.Default
    private Integer ordinalPosition = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
