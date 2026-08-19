package com.aedmp.platform.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, Long> {

    Optional<ProcessingJob> findByIdempotencyKey(String idempotencyKey);

    List<ProcessingJob> findByDatasetVersionIdOrderByCreatedAtDesc(Long datasetVersionId);

    List<ProcessingJob> findByStatusOrderByCreatedAtDesc(JobStatus status);

    long countByStatus(JobStatus status);

    long countByCreatedAtAfter(Instant since);

    List<ProcessingJob> findByStatus(JobStatus status);
}
