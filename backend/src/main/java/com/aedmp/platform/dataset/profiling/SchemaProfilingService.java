package com.aedmp.platform.dataset.profiling;

import com.aedmp.platform.dataset.csv.CsvTable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Infers an observed type per column from actual values: INTEGER, DECIMAL, DATE, BOOLEAN, or the
 * STRING fallback. A column is only typed as something stricter than STRING if every non-blank
 * value in the file parses as that type - one bad row is enough to fall back to STRING, which is
 * then surfaced as a validity finding rather than silently mistyping the column.
 */
@Service
public class SchemaProfilingService {

    private static final int MAX_SAMPLE_VALUES = 5;
    private static final int MAX_SAMPLE_ROWS = 10;

    public ProfiledSchema profile(CsvTable table) {
        List<ProfiledColumn> columns = new ArrayList<>();
        for (int col = 0; col < table.header().size(); col++) {
            columns.add(profileColumn(table, col));
        }
        List<List<String>> sampleRows = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_SAMPLE_ROWS, table.rows().size()); i++) {
            sampleRows.add(List.of(table.rows().get(i)));
        }
        return new ProfiledSchema(table.rows().size(), columns, sampleRows);
    }

    private ProfiledColumn profileColumn(CsvTable table, int colIndex) {
        String columnName = table.header().get(colIndex);
        Set<String> distinct = new LinkedHashSet<>();
        long nullCount = 0;
        boolean allInteger = true;
        boolean allDecimal = true;
        boolean allDate = true;
        boolean allBoolean = true;
        boolean anyValue = false;

        for (String[] row : table.rows()) {
            String raw = row[colIndex];
            String value = raw == null ? "" : raw.trim();
            if (value.isEmpty()) {
                nullCount++;
                continue;
            }
            anyValue = true;
            distinct.add(value);
            if (allInteger && !isInteger(value)) {
                allInteger = false;
            }
            if (allDecimal && !isDecimal(value)) {
                allDecimal = false;
            }
            if (allDate && !isDate(value)) {
                allDate = false;
            }
            if (allBoolean && !isBoolean(value)) {
                allBoolean = false;
            }
        }

        String observedType;
        if (!anyValue) {
            observedType = "STRING";
        } else if (allInteger) {
            observedType = "INTEGER";
        } else if (allDecimal) {
            observedType = "DECIMAL";
        } else if (allDate) {
            observedType = "DATE";
        } else if (allBoolean) {
            observedType = "BOOLEAN";
        } else {
            observedType = "STRING";
        }

        String sampleValues = distinct.stream().limit(MAX_SAMPLE_VALUES).reduce((a, b) -> a + ", " + b).orElse("");

        return new ProfiledColumn(columnName, colIndex + 1, observedType, nullCount > 0, distinct.size(), nullCount, sampleValues);
    }

    private boolean isInteger(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isDecimal(String value) {
        try {
            new BigDecimal(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isDate(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean isBoolean(String value) {
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
    }
}
