package com.aedmp.platform.job.dto;

import com.aedmp.platform.job.ProcessingJob;

import java.time.Instant;

public record JobResponse(
        Long id,
        Long datasetVersionId,
        String jobType,
        String status,
        Integer retryCount,
        Integer maxRetries,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        String createdByEmail,
        Instant createdAt
) {
    public static JobResponse from(ProcessingJob j) {
        return new JobResponse(j.getId(), j.getDatasetVersion().getId(), j.getJobType().name(), j.getStatus().name(),
                j.getRetryCount(), j.getMaxRetries(), j.getErrorMessage(), j.getStartedAt(), j.getCompletedAt(),
                j.getCreatedBy().getEmail(), j.getCreatedAt());
    }
}
