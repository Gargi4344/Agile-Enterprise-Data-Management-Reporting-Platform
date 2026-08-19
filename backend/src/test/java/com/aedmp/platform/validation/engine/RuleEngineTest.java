package com.aedmp.platform.validation.engine;

import com.aedmp.platform.dataset.csv.CsvTable;
import com.aedmp.platform.user.User;
import com.aedmp.platform.validation.ExpressionType;
import com.aedmp.platform.validation.RuleFamily;
import com.aedmp.platform.validation.RuleSeverity;
import com.aedmp.platform.validation.ValidationRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineTest {

    private final RuleEngine engine = new RuleEngine();

    private ValidationRule rule(Long id, RuleFamily family, RuleSeverity severity, String column,
                                 ExpressionType type, String expression) {
        return ValidationRule.builder()
                .id(id)
                .name("rule-" + id)
                .ruleFamily(family)
                .severity(severity)
                .columnName(column)
                .expressionType(type)
                .expression(expression)
                .active(true)
                .owner(User.builder().id(1L).email("owner@demo.aedmp.local").build())
                .build();
    }

    @Test
    void notNullRuleFlagsBlankValues() {
        CsvTable table = new CsvTable(List.of("email"), List.of(
                new String[]{"a@example.com"}, new String[]{""}, new String[]{"b@example.com"}));
        ValidationRule rule = rule(1L, RuleFamily.COMPLETENESS, RuleSeverity.BLOCKER, "email", ExpressionType.NOT_NULL, "");

        List<RuleFinding> findings = engine.evaluate(List.of(rule), table);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).rowNumber()).isEqualTo(3L); // header is row 1, so second data row is row 3
        assertThat(findings.get(0).severity()).isEqualTo(RuleSeverity.BLOCKER);
    }

    @Test
    void uniqueRuleFlagsOnlyRepeatOccurrences() {
        CsvTable table = new CsvTable(List.of("order_id"), List.of(
                new String[]{"A"}, new String[]{"A"}, new String[]{"B"}, new String[]{"A"}));
        ValidationRule rule = rule(2L, RuleFamily.UNIQUENESS, RuleSeverity.BLOCKER, "order_id", ExpressionType.UNIQUE, "");

        List<RuleFinding> findings = engine.evaluate(List.of(rule), table);

        assertThat(findings).hasSize(2);
        assertThat(findings).allMatch(f -> f.sampleValue().equals("A"));
    }

    @Test
    void regexRuleIgnoresBlankValuesButFlagsMalformedOnes() {
        CsvTable table = new CsvTable(List.of("email"), List.of(
                new String[]{"valid@example.com"}, new String[]{"not-an-email"}, new String[]{""}));
        ValidationRule rule = rule(3L, RuleFamily.VALIDITY, RuleSeverity.WARNING, "email", ExpressionType.REGEX,
                "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

        List<RuleFinding> findings = engine.evaluate(List.of(rule), table);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).severity()).isEqualTo(RuleSeverity.WARNING);
    }

    @Test
    void rangeMinRuleFlagsValuesBelowThreshold() {
        CsvTable table = new CsvTable(List.of("amount"), List.of(
                new String[]{"10.00"}, new String[]{"-5.00"}, new String[]{"0.00"}));
        ValidationRule rule = rule(4L, RuleFamily.VALIDITY, RuleSeverity.BLOCKER, "amount", ExpressionType.RANGE_MIN, "0");

        List<RuleFinding> findings = engine.evaluate(List.of(rule), table);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).sampleValue()).isEqualTo("-5.00");
    }

    @Test
    void enumRuleFlagsValuesOutsideAllowedSet() {
        CsvTable table = new CsvTable(List.of("status"), List.of(
                new String[]{"PENDING"}, new String[]{"UNKNOWN"}, new String[]{"SHIPPED"}));
        ValidationRule rule = rule(5L, RuleFamily.VALIDITY, RuleSeverity.BLOCKER, "status", ExpressionType.ENUM,
                "PENDING,SHIPPED,DELIVERED");

        List<RuleFinding> findings = engine.evaluate(List.of(rule), table);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).sampleValue()).isEqualTo("UNKNOWN");
    }

    @Test
    void missingColumnProducesSingleSchemaFinding() {
        CsvTable table = new CsvTable(List.of("order_id"), List.<String[]>of(new String[]{"A"}));
        ValidationRule rule = rule(6L, RuleFamily.SCHEMA, RuleSeverity.BLOCKER, "customer_email",
                ExpressionType.NOT_NULL, "");

        List<RuleFinding> findings = engine.evaluate(List.of(rule), table);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).rowNumber()).isNull();
        assertThat(findings.get(0).message()).contains("missing");
    }

    @Test
    void engineIsDeterministicAcrossRepeatedRuns() {
        CsvTable table = new CsvTable(List.of("order_id", "amount"), List.of(
                new String[]{"A", "10"}, new String[]{"A", "-1"}, new String[]{"B", "5"}));
        List<ValidationRule> rules = List.of(
                rule(7L, RuleFamily.UNIQUENESS, RuleSeverity.BLOCKER, "order_id", ExpressionType.UNIQUE, ""),
                rule(8L, RuleFamily.VALIDITY, RuleSeverity.BLOCKER, "amount", ExpressionType.RANGE_MIN, "0"));

        List<RuleFinding> first = engine.evaluate(rules, table);
        List<RuleFinding> second = engine.evaluate(rules, table);

        assertThat(first).hasSameSizeAs(second);
        assertThat(first).containsExactlyInAnyOrderElementsOf(second);
    }
}
