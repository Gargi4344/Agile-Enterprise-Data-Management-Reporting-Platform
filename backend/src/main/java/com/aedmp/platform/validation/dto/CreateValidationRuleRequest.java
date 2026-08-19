package com.aedmp.platform.validation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateValidationRuleRequest(
        @NotBlank String name,
        String description,
        @NotNull String ruleFamily,
        String severity,
        Long datasetId,
        String columnName,
        @NotNull String expressionType,
        String expression,
        BigDecimal weight
) {
}
