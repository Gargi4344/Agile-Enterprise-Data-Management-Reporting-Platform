package com.aedmp.platform.dataset.dto;

import com.aedmp.platform.dataset.Dataset;

import java.time.Instant;

public record DatasetResponse(
        Long id,
        String name,
        String description,
        String ownerEmail,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static DatasetResponse from(Dataset dataset) {
        return new DatasetResponse(dataset.getId(), dataset.getName(), dataset.getDescription(),
                dataset.getOwner().getEmail(), dataset.getStatus().name(), dataset.getCreatedAt(), dataset.getUpdatedAt());
    }
}
