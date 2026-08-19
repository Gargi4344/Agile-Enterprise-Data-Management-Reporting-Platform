package com.aedmp.platform.validation.engine;

import com.aedmp.platform.validation.RuleSeverity;

public record RuleFinding(
        Long ruleId,
        Long rowNumber,
        String columnName,
        RuleSeverity severity,
        boolean passed,
        String message,
        String sampleValue
) {
}
