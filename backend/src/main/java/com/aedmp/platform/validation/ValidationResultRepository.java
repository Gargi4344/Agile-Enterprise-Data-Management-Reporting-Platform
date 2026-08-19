package com.aedmp.platform.validation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidationResultRepository extends JpaRepository<ValidationResult, Long> {

    Page<ValidationResult> findByValidationRunIdAndPassed(Long runId, Boolean passed, Pageable pageable);

    Page<ValidationResult> findByValidationRunIdAndSeverity(Long runId, RuleSeverity severity, Pageable pageable);

    Page<ValidationResult> findByValidationRunIdAndPassedAndSeverity(
            Long runId, Boolean passed, RuleSeverity severity, Pageable pageable);

    Page<ValidationResult> findByValidationRunId(Long runId, Pageable pageable);

    List<ValidationResult> findByValidationRunIdAndPassedFalse(Long runId);

    long countByValidationRunIdAndPassedFalse(Long runId);

    long countBySeverityAndPassedFalse(RuleSeverity severity);
}
