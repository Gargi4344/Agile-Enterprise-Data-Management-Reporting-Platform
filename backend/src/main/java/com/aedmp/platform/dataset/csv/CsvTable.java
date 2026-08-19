package com.aedmp.platform.dataset.csv;

import java.util.List;

/** In-memory parse of a dataset version's CSV file, shared by schema profiling and the validation engine. */
public record CsvTable(List<String> header, List<String[]> rows) {

    public int columnIndex(String columnName) {
        return header.indexOf(columnName);
    }
}
