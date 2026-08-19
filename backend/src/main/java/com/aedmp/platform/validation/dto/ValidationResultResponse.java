package com.aedmp.platform.validation.dto;

import com.aedmp.platform.validation.ValidationResult;

public record ValidationResultResponse(
        Long id,
        Long ruleId,
        String ruleName,
        Long rowNumber,
        String columnName,
        String severity,
        Boolean passed,
        String message,
        String sampleValue
) {
    public static ValidationResultResponse from(ValidationResult r) {
        return new ValidationResultResponse(r.getId(), r.getRule() != null ? r.getRule().getId() : null,
                r.getRule() != null ? r.getRule().getName() : null, r.getRowNumber(), r.getColumnName(),
                r.getSeverity().name(), r.getPassed(), r.getMessage(), r.getSampleValue());
    }
}
