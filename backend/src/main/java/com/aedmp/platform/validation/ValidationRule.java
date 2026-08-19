package com.aedmp.platform.validation;

import com.aedmp.platform.dataset.Dataset;
import com.aedmp.platform.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "validation_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_family", nullable = false, length = 20)
    private RuleFamily ruleFamily;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RuleSeverity severity = RuleSeverity.BLOCKER;

    /** Null means the rule is global and applies to every dataset. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;

    @Column(name = "column_name")
    private String columnName;

    @Enumerated(EnumType.STRING)
    @Column(name = "expression_type", nullable = false, length = 30)
    private ExpressionType expressionType;

    @Column(nullable = false, length = 2000)
    private String expression;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal weight = BigDecimal.ONE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

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
