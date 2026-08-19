package com.aedmp.platform.job.transform;

import com.aedmp.platform.dataset.csv.CsvTable;
import com.aedmp.platform.job.TransformationRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransformationExecutorTest {

    private final TransformationExecutor executor = new TransformationExecutor();

    private TransformationRule rule(String type, String column, int ordinal) {
        return TransformationRule.builder()
                .id((long) ordinal)
                .name(type + "-" + column)
                .transformationType(type)
                .expression(column)
                .ordinalPosition(ordinal)
                .version(1)
                .active(true)
                .build();
    }

    @Test
    void trimsAndUppercasesInDeclaredOrder() {
        CsvTable input = new CsvTable(List.of("email", "status"), List.<String[]>of(
                new String[]{"  jane@example.com  ", "pending"}));
        List<TransformationRule> rules = List.of(
                rule("TRIM", "email", 0),
                rule("UPPERCASE", "status", 1));

        CsvTable output = executor.apply(input, rules);

        assertThat(output.rows().get(0)[0]).isEqualTo("jane@example.com");
        assertThat(output.rows().get(0)[1]).isEqualTo("PENDING");
    }

    @Test
    void doesNotMutateInputTable() {
        CsvTable input = new CsvTable(List.of("name"), List.<String[]>of(new String[]{"lower"}));
        List<TransformationRule> rules = List.of(rule("UPPERCASE", "name", 0));

        executor.apply(input, rules);

        assertThat(input.rows().get(0)[0]).isEqualTo("lower");
    }

    @Test
    void sameInputAndRulesProduceIdenticalOutputEveryTime() {
        CsvTable input = new CsvTable(List.of("name"), List.of(new String[]{"Jane"}, new String[]{"Sam"}));
        List<TransformationRule> rules = List.of(rule("LOWERCASE", "name", 0));

        CsvTable first = executor.apply(input, rules);
        CsvTable second = executor.apply(input, rules);

        assertThat(first.rows().get(0)[0]).isEqualTo(second.rows().get(0)[0]);
        assertThat(first.rows().get(1)[0]).isEqualTo(second.rows().get(1)[0]);
    }

    @Test
    void unknownColumnIsSkippedWithoutError() {
        CsvTable input = new CsvTable(List.of("name"), List.<String[]>of(new String[]{"Jane"}));
        List<TransformationRule> rules = List.of(rule("UPPERCASE", "does_not_exist", 0));

        CsvTable output = executor.apply(input, rules);

        assertThat(output.rows().get(0)[0]).isEqualTo("Jane");
    }
}
