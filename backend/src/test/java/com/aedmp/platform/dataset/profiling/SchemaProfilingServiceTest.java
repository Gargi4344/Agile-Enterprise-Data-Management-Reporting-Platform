package com.aedmp.platform.dataset.profiling;

import com.aedmp.platform.dataset.csv.CsvTable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaProfilingServiceTest {

    private final SchemaProfilingService service = new SchemaProfilingService();

    @Test
    void infersIntegerColumnWhenAllValuesAreWholeNumbers() {
        CsvTable table = new CsvTable(List.of("qty"), List.of(
                new String[]{"1"}, new String[]{"2"}, new String[]{"3"}));

        ProfiledSchema schema = service.profile(table);

        assertThat(schema.columns().get(0).observedType()).isEqualTo("INTEGER");
        assertThat(schema.rowCount()).isEqualTo(3);
    }

    @Test
    void fallsBackToStringWhenOneValueBreaksTheStrictType() {
        CsvTable table = new CsvTable(List.of("qty"), List.of(
                new String[]{"1"}, new String[]{"not-a-number"}, new String[]{"3"}));

        ProfiledSchema schema = service.profile(table);

        assertThat(schema.columns().get(0).observedType()).isEqualTo("STRING");
    }

    @Test
    void countsNullsAndDistinctValuesPerColumn() {
        CsvTable table = new CsvTable(List.of("region"), List.of(
                new String[]{"NA"}, new String[]{""}, new String[]{"NA"}, new String[]{"EMEA"}));

        ProfiledSchema schema = service.profile(table);
        ProfiledColumn column = schema.columns().get(0);

        assertThat(column.nullCount()).isEqualTo(1);
        assertThat(column.distinctCount()).isEqualTo(2);
        assertThat(column.nullable()).isTrue();
    }

    @Test
    void detectsDecimalAndDateColumns() {
        CsvTable table = new CsvTable(List.of("amount", "order_date"), List.of(
                new String[]{"19.99", "2026-05-01"}, new String[]{"4.50", "2026-05-02"}));

        ProfiledSchema schema = service.profile(table);

        assertThat(schema.columns().get(0).observedType()).isEqualTo("DECIMAL");
        assertThat(schema.columns().get(1).observedType()).isEqualTo("DATE");
    }

    @Test
    void emptyDatasetProducesZeroRowsWithoutError() {
        CsvTable table = new CsvTable(List.of("a", "b"), List.of());

        ProfiledSchema schema = service.profile(table);

        assertThat(schema.rowCount()).isZero();
        assertThat(schema.columns()).hasSize(2);
        assertThat(schema.columns().get(0).observedType()).isEqualTo("STRING");
    }
}
