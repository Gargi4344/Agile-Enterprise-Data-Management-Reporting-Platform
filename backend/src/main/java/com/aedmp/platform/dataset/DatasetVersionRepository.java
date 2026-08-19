package com.aedmp.platform.dataset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatasetVersionRepository extends JpaRepository<DatasetVersion, Long> {

    List<DatasetVersion> findByDatasetIdOrderByVersionNumberDesc(Long datasetId);

    Optional<DatasetVersion> findFirstByDatasetIdOrderByVersionNumberDesc(Long datasetId);

    Optional<DatasetVersion> findByDatasetIdAndChecksumSha256(Long datasetId, String checksum);

    long countByStatus(DatasetVersionStatus status);

    long countByCreatedAtAfter(java.time.Instant since);
}
