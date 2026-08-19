package com.aedmp.platform.validation.dto;

import java.math.BigDecimal;

public record UpdateValidationRuleRequest(
        String name,
        String description,
        String severity,
        String columnName,
        String expressionType,
        String expression,
        Boolean active,
        BigDecimal weight
) {
}
