package com.aedmp.platform.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record KpiOverviewResponse(
        long datasetsRegistered,
        long activeDatasetVersions,
        long validationRuns,
        BigDecimal qualityRate,
        long failedRuleChecks,
        long recordsProcessed,
        Double averageProcessingDurationSeconds,
        long failedJobs,
        long successfulJobs,
        long reportsGenerated,
        List<RecentActivityItem> recentActivity,
        Instant computedAt
) {
    public record RecentActivityItem(String action, String targetType, String targetId, String result, Instant createdAt) {
    }
}
