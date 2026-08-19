package com.aedmp.platform.validation.dto;

import com.aedmp.platform.validation.ValidationRule;

import java.math.BigDecimal;
import java.time.Instant;

public record ValidationRuleResponse(
        Long id,
        String name,
        String description,
        String ruleFamily,
        String severity,
        Long datasetId,
        String columnName,
        String expressionType,
        String expression,
        Boolean active,
        BigDecimal weight,
        String ownerEmail,
        Integer version,
        Instant createdAt,
        Instant updatedAt
) {
    public static ValidationRuleResponse from(ValidationRule r) {
        return new ValidationRuleResponse(r.getId(), r.getName(), r.getDescription(), r.getRuleFamily().name(),
                r.getSeverity().name(), r.getDataset() != null ? r.getDataset().getId() : null, r.getColumnName(),
                r.getExpressionType().name(), r.getExpression(), r.getActive(), r.getWeight(),
                r.getOwner().getEmail(), r.getVersion(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
