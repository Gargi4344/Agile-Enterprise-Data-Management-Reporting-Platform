package com.aedmp.platform.validation.dto;

import jakarta.validation.constraints.NotNull;

public record CreateValidationRunRequest(@NotNull Long datasetVersionId) {
}
