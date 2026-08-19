package com.aedmp.platform.report.dto;

import jakarta.validation.constraints.NotNull;

public record CreateReportRequest(@NotNull Long datasetVersionId, String reportType) {
}
