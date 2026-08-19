package com.aedmp.platform.validation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ValidationRunRepository extends JpaRepository<ValidationRun, Long> {

    List<ValidationRun> findByDatasetVersionIdOrderByStartedAtDesc(Long datasetVersionId);

    long countByStatus(ValidationRunStatus status);

    long countByCreatedAtAfter(Instant since);

    List<ValidationRun> findByQualityScoreIsNotNull();
}
