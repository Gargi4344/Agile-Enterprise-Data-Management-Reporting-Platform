package com.aedmp.platform.dataset.dto;

import com.aedmp.platform.dataset.DatasetVersion;

import java.time.Instant;

public record DatasetVersionResponse(
        Long id,
        Long datasetId,
        Integer versionNumber,
        String sourceFilename,
        String sourceSystem,
        String checksumSha256,
        Long fileSizeBytes,
        Long rowCount,
        String status,
        String uploadedByEmail,
        Instant uploadedAt
) {
    public static DatasetVersionResponse from(DatasetVersion v) {
        return new DatasetVersionResponse(v.getId(), v.getDataset().getId(), v.getVersionNumber(),
                v.getSourceFilename(), v.getSourceSystem(), v.getChecksumSha256(), v.getFileSizeBytes(),
                v.getRowCount(), v.getStatus().name(), v.getUploadedBy().getEmail(), v.getUploadedAt());
    }
}
