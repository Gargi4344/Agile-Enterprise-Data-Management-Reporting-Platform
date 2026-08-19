package com.aedmp.platform.dataset.profiling;

public record ProfiledColumn(
        String columnName,
        int ordinalPosition,
        String observedType,
        boolean nullable,
        long distinctCount,
        long nullCount,
        String sampleValues
) {
}
