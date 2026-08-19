package com.aedmp.platform.job.transform;

import com.aedmp.platform.dataset.csv.CsvTable;
import com.aedmp.platform.job.TransformationRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure, deterministic row transformation: same input table + same active rule set always
 * produces the same output table. Supported transformation_type values: TRIM, UPPERCASE,
 * LOWERCASE, each targeting the column named in the rule's `expression`.
 */
@Component
public class TransformationExecutor {

    public CsvTable apply(CsvTable input, List<TransformationRule> rules) {
        List<String> header = input.header();
        List<String[]> rows = new ArrayList<>();
        for (String[] row : input.rows()) {
            rows.add(row.clone());
        }

        for (TransformationRule rule : rules) {
            int colIndex = header.indexOf(rule.getExpression().trim());
            if (colIndex < 0) {
                continue;
            }
            for (String[] row : rows) {
                row[colIndex] = applyOne(rule.getTransformationType(), row[colIndex]);
            }
        }
        return new CsvTable(header, rows);
    }

    private String applyOne(String type, String value) {
        if (value == null) {
            return null;
        }
        return switch (type.toUpperCase()) {
            case "TRIM" -> value.trim();
            case "UPPERCASE" -> value.toUpperCase();
            case "LOWERCASE" -> value.toLowerCase();
            default -> value;
        };
    }
}
