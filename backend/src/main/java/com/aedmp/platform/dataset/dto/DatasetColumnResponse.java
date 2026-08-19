package com.aedmp.platform.dataset.dto;

import com.aedmp.platform.dataset.DatasetColumn;

public record DatasetColumnResponse(
        Long id,
        String columnName,
        String expectedType,
        String observedType,
        Integer ordinalPosition,
        Boolean nullable,
        Long distinctCount,
        Long nullCount,
        String sampleValues
) {
    public static DatasetColumnResponse from(DatasetColumn c) {
        return new DatasetColumnResponse(c.getId(), c.getColumnName(), c.getExpectedType(), c.getObservedType(),
                c.getOrdinalPosition(), c.getNullableObserved(), c.getDistinctCount(), c.getNullCount(), c.getSampleValues());
    }
}
