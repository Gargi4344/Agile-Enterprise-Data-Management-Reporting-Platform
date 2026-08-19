package com.aedmp.platform.validation;

import com.aedmp.platform.audit.AuditResult;
import com.aedmp.platform.audit.AuditService;
import com.aedmp.platform.common.exception.NotFoundException;
import com.aedmp.platform.dataset.DatasetVersion;
import com.aedmp.platform.dataset.DatasetVersionService;
import com.aedmp.platform.dataset.DatasetVersionStatus;
import com.aedmp.platform.dataset.csv.CsvDatasetReader;
import com.aedmp.platform.dataset.csv.CsvTable;
import com.aedmp.platform.dataset.storage.FileStorageService;
import com.aedmp.platform.security.AuthenticatedUser;
import com.aedmp.platform.validation.engine.RuleEngine;
import com.aedmp.platform.validation.engine.RuleFinding;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ValidationRunService {

    private final ValidationRunRepository runRepository;
    private final ValidationResultRepository resultRepository;
    private final ValidationRuleRepository ruleRepository;
    private final DatasetVersionService versionService;
    private final FileStorageService fileStorageService;
    private final CsvDatasetReader csvDatasetReader;
    private final RuleEngine ruleEngine;
    private final AuditService auditService;

    public List<ValidationRun> listForVersion(Long versionId) {
        return runRepository.findByDatasetVersionIdOrderByStartedAtDesc(versionId);
    }

    public ValidationRun get(Long id) {
        return runRepository.findById(id).orElseThrow(() -> new NotFoundException("Validation run " + id + " not found"));
    }

    public Page<ValidationResult> results(Long runId, Boolean passed, RuleSeverity severity, Pageable pageable) {
        if (passed != null && severity != null) {
            return resultRepository.findByValidationRunIdAndPassedAndSeverity(runId, passed, severity, pageable);
        } else if (passed != null) {
            return resultRepository.findByValidationRunIdAndPassed(runId, passed, pageable);
        } else if (severity != null) {
            return resultRepository.findByValidationRunIdAndSeverity(runId, severity, pageable);
        }
        return resultRepository.findByValidationRunId(runId, pageable);
    }

    @Transactional
    public ValidationRun runValidation(Long datasetVersionId, AuthenticatedUser principal) {
        DatasetVersion version = versionService.get(datasetVersionId);
        versionService.transition(datasetVersionId, DatasetVersionStatus.VALIDATING);

        ValidationRun run = runRepository.save(ValidationRun.builder()
                .datasetVersion(version)
                .status(ValidationRunStatus.RUNNING)
                .triggeredBy(principal.getUser())
                .startedAt(Instant.now())
                .build());

        CsvTable table;
        try {
            table = csvDatasetReader.read(fileStorageService.load(version.getStorageLocation()));
        } catch (IOException e) {
            run.setStatus(ValidationRunStatus.FAILED);
            run.setCompletedAt(Instant.now());
            runRepository.save(run);
            versionService.transition(datasetVersionId, DatasetVersionStatus.REJECTED);
            auditService.record("VALIDATION_RUN", "VALIDATION_RUN", String.valueOf(run.getId()), AuditResult.FAILURE,
                    null, Map.of("reason", "storage read failure"));
            return run;
        }

        List<ValidationRule> rules = ruleRepository.findActiveRulesForDataset(version.getDataset().getId());
        List<RuleFinding> findings = ruleEngine.evaluate(rules, table);

        Set<Long> rejectedRows = new HashSet<>();
        boolean anyWarning = false;
        for (RuleFinding finding : findings) {
            resultRepository.save(ValidationResult.builder()
                    .validationRun(run)
                    .rule(ruleById(rules, finding.ruleId()))
                    .rowNumber(finding.rowNumber())
                    .columnName(finding.columnName())
                    .severity(finding.severity())
                    .passed(false)
                    .message(finding.message())
                    .sampleValue(finding.sampleValue())
                    .build());
            if (finding.severity() == RuleSeverity.BLOCKER && finding.rowNumber() != null) {
                rejectedRows.add(finding.rowNumber());
            }
            if (finding.severity() == RuleSeverity.WARNING) {
                anyWarning = true;
            }
        }

        boolean missingColumnBlocker = findings.stream()
                .anyMatch(f -> f.rowNumber() == null && f.severity() == RuleSeverity.BLOCKER);

        long total = table.rows().size();
        long rejected = missingColumnBlocker ? total : rejectedRows.size();
        long accepted = total - rejected;
        BigDecimal qualityScore = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(accepted).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        boolean hasBlockerFailure = missingColumnBlocker || !rejectedRows.isEmpty();
        ValidationRunStatus runStatus = hasBlockerFailure
                ? ValidationRunStatus.FAILED
                : (anyWarning ? ValidationRunStatus.PASSED_WITH_WARNINGS : ValidationRunStatus.PASSED);
        DatasetVersionStatus targetStatus = hasBlockerFailure ? DatasetVersionStatus.REJECTED : DatasetVersionStatus.READY;

        run.setTotalRecords(total);
        run.setAcceptedRecords(accepted);
        run.setRejectedRecords(rejected);
        run.setQualityScore(qualityScore);
        run.setStatus(runStatus);
        run.setCompletedAt(Instant.now());
        ValidationRun saved = runRepository.save(run);

        versionService.transition(datasetVersionId, targetStatus);

        auditService.record("VALIDATION_RUN", "VALIDATION_RUN", String.valueOf(saved.getId()),
                hasBlockerFailure ? AuditResult.FAILURE : AuditResult.SUCCESS, null,
                Map.of("datasetVersionId", datasetVersionId, "qualityScore", qualityScore,
                        "totalRecords", total, "rejectedRecords", rejected));

        return saved;
    }

    private ValidationRule ruleById(List<ValidationRule> rules, Long ruleId) {
        if (ruleId == null) {
            return null;
        }
        return rules.stream().filter(r -> r.getId().equals(ruleId)).findFirst().orElse(null);
    }
}
