package com.aedmp.platform.job.dto;

import jakarta.validation.constraints.NotNull;

public record CreateJobRequest(@NotNull Long datasetVersionId, @NotNull String jobType) {
}
