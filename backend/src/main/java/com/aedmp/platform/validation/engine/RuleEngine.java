package com.aedmp.platform.validation.engine;

import com.aedmp.platform.dataset.csv.CsvTable;
import com.aedmp.platform.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Deterministic, side-effect-free evaluation of a rule catalog against a parsed CSV table.
 * Only findings for FAILED checks are returned (see docs/state-machine.md quality-score note) -
 * callers persist these as validation_results and use dataset row count minus distinct failing
 * rows (weighted by severity) to compute accepted/rejected counts.
 */
@Component
public class RuleEngine {

    public List<RuleFinding> evaluate(List<ValidationRule> rules, CsvTable table) {
        List<RuleFinding> findings = new ArrayList<>();
        for (ValidationRule rule : rules) {
            findings.addAll(evaluateRule(rule, table));
        }
        return findings;
    }

    private List<RuleFinding> evaluateRule(ValidationRule rule, CsvTable table) {
        int colIndex = rule.getColumnName() == null ? -1 : table.columnIndex(rule.getColumnName());

        if (rule.getExpressionType() == com.aedmp.platform.validation.ExpressionType.COLUMN_PRESENT
                || (colIndex < 0 && rule.getColumnName() != null)) {
            return List.of(new RuleFinding(rule.getId(), null, rule.getColumnName(), rule.getSeverity(), false,
                    "Required column '" + rule.getColumnName() + "' is missing from the uploaded file", null));
        }

        return switch (rule.getExpressionType()) {
            case NOT_NULL -> notNull(rule, table, colIndex);
            case UNIQUE -> unique(rule, table, colIndex);
            case REGEX -> regex(rule, table, colIndex);
            case RANGE_MIN -> rangeMin(rule, table, colIndex);
            case RANGE_MAX -> rangeMax(rule, table, colIndex);
            case ENUM -> enumCheck(rule, table, colIndex);
            case TYPE_MATCH -> typeMatch(rule, table, colIndex);
            case COLUMN_PRESENT -> List.of();
        };
    }

    private List<RuleFinding> notNull(ValidationRule rule, CsvTable table, int colIndex) {
        List<RuleFinding> findings = new ArrayList<>();
        for (int i = 0; i < table.rows().size(); i++) {
            String value = table.rows().get(i)[colIndex];
            if (value == null || value.isBlank()) {
                findings.add(fail(rule, i, "Value is required but was missing", null));
            }
        }
        return findings;
    }

    private List<RuleFinding> unique(ValidationRule rule, CsvTable table, int colIndex) {
        Map<String, Integer> firstSeenAtRow = new HashMap<>();
        List<RuleFinding> findings = new ArrayList<>();
        for (int i = 0; i < table.rows().size(); i++) {
            String value = table.rows().get(i)[colIndex];
            if (value == null || value.isBlank()) {
                continue;
            }
            if (firstSeenAtRow.containsKey(value)) {
                findings.add(fail(rule, i, "Duplicate value '" + value + "' (first seen at row "
                        + (firstSeenAtRow.get(value) + 2) + ")", value));
            } else {
                firstSeenAtRow.put(value, i);
            }
        }
        return findings;
    }

    private List<RuleFinding> regex(ValidationRule rule, CsvTable table, int colIndex) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(rule.getExpression());
        } catch (PatternSyntaxException e) {
            return List.of();
        }
        List<RuleFinding> findings = new ArrayList<>();
        for (int i = 0; i < table.rows().size(); i++) {
            String value = table.rows().get(i)[colIndex];
            if (value != null && !value.isBlank() && !pattern.matcher(value).matches()) {
                findings.add(fail(rule, i, "Value '" + value + "' does not match the expected format", value));
            }
        }
        return findings;
    }

    private List<RuleFinding> rangeMin(ValidationRule rule, CsvTable table, int colIndex) {
        BigDecimal min = parseDecimalOrNull(rule.getExpression());
        if (min == null) {
            return List.of();
        }
        List<RuleFinding> findings = new ArrayList<>();
        for (int i = 0; i < table.rows().size(); i++) {
            String value = table.rows().get(i)[colIndex];
            BigDecimal parsed = parseDecimalOrNull(value);
            if (parsed != null && parsed.compareTo(min) < 0) {
                findings.add(fail(rule, i, "Value " + value + " is below the minimum allowed (" + min + ")", value));
            }
        }
        return findings;
    }

    private List<RuleFinding> rangeMax(ValidationRule rule, CsvTable table, int colIndex) {
        BigDecimal max = parseDecimalOrNull(rule.getExpression());
        if (max == null) {
            return List.of();
        }
        List<RuleFinding> findings = new ArrayList<>();
        for (int i = 0; i < table.rows().size(); i++) {
            String value = table.rows().get(i)[colIndex];
            BigDecimal parsed = parseDecimalOrNull(value);
            if (parsed != null && parsed.compareTo(max) > 0) {
                findings.add(fail(rule, i, "Value " + value + " exceeds the maximum allowed (" + max + ")", value));
            }
        }
        return findings;
    }

    private List<RuleFinding> enumCheck(ValidationRule rule, CsvTable table, int colIndex) {
        Set<String> allowed = new HashSet<>();
        for (String v : rule.getExpression().split(",")) {
            allowed.add(v.trim());
        }
        List<RuleFinding> findings = new ArrayList<>();
        for (int i = 0; i < table.rows().size(); i++) {
            String value = table.rows().get(i)[colIndex];
            if (value != null && !value.isBlank() && !allowed.contains(value.trim())) {
                findings.add(fail(rule, i, "Value '" + value + "' is not one of the allowed values", value));
            }
        }
        return findings;
    }

    private List<RuleFinding> typeMatch(ValidationRule rule, CsvTable table, int colIndex) {
        String expectedType = rule.getExpression().trim().toUpperCase();
        List<RuleFinding> findings = new ArrayList<>();
        for (int i = 0; i < table.rows().size(); i++) {
            String value = table.rows().get(i)[colIndex];
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!matchesType(value, expectedType)) {
                findings.add(fail(rule, i, "Value '" + value + "' is not a valid " + expectedType, value));
            }
        }
        return findings;
    }

    private boolean matchesType(String value, String type) {
        try {
            switch (type) {
                case "INTEGER" -> Long.parseLong(value);
                case "DECIMAL" -> new BigDecimal(value);
                case "DATE" -> java.time.LocalDate.parse(value);
                case "BOOLEAN" -> {
                    return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
                }
                default -> {
                    return true;
                }
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private BigDecimal parseDecimalOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private RuleFinding fail(ValidationRule rule, int rowIndex, String message, String sampleValue) {
        return new RuleFinding(rule.getId(), (long) (rowIndex + 2), rule.getColumnName(), rule.getSeverity(), false, message, sampleValue);
    }
}
