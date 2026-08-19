package com.aedmp.platform.report.dto;

import com.aedmp.platform.report.Report;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record ReportResponse(
        Long id,
        String name,
        Long datasetId,
        Long datasetVersionId,
        String reportType,
        String status,
        String generatedByEmail,
        Instant generatedAt,
        JsonNode parameters,
        JsonNode summaryMetrics
) {
    public static ReportResponse from(Report r) {
        return new ReportResponse(r.getId(), r.getName(), r.getDataset() != null ? r.getDataset().getId() : null,
                r.getDatasetVersion() != null ? r.getDatasetVersion().getId() : null, r.getReportType(),
                r.getStatus().name(), r.getGeneratedBy().getEmail(), r.getGeneratedAt(), r.getParameters(), r.getSummaryMetrics());
    }
}
