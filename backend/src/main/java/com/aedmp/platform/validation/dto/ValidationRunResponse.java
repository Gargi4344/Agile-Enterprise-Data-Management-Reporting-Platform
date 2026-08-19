package com.aedmp.platform.validation.dto;

import com.aedmp.platform.validation.ValidationRun;

import java.math.BigDecimal;
import java.time.Instant;

public record ValidationRunResponse(
        Long id,
        Long datasetVersionId,
        String status,
        String triggeredByEmail,
        Instant startedAt,
        Instant completedAt,
        Long totalRecords,
        Long acceptedRecords,
        Long rejectedRecords,
        BigDecimal qualityScore
) {
    public static ValidationRunResponse from(ValidationRun r) {
        return new ValidationRunResponse(r.getId(), r.getDatasetVersion().getId(), r.getStatus().name(),
                r.getTriggeredBy().getEmail(), r.getStartedAt(), r.getCompletedAt(), r.getTotalRecords(),
                r.getAcceptedRecords(), r.getRejectedRecords(), r.getQualityScore());
    }
}
