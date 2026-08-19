package com.aedmp.platform.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByDatasetIdOrderByGeneratedAtDesc(Long datasetId);

    long countByCreatedAtAfter(Instant since);
}
